package cf.maybelambda.httpvalidator.springboot.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.springframework.core.env.Environment;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cf.maybelambda.httpvalidator.springboot.service.JwtAuthenticationService.BEARER_PREFIX;
import static cf.maybelambda.httpvalidator.springboot.service.JwtAuthenticationService.SECRET_PROPERTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class JwtAuthenticationServiceTests {
    private JwtAuthenticationService authServ;
    private String nonExpiredToken;
    private final Environment env = mock(Environment.class);
    private final Logger logger = mock(Logger.class);

    @BeforeEach
    void setUp() {
        String encodedTestsKey = "1Nu1KUNoBSlaOHyH1En88wGDvSM3qV3zdE9tcASGu87JsJf33UjofHGFW42U5ZRypAJz81OWydStVuX+qilW3Q==";
        this.authServ = new JwtAuthenticationService();

        this.authServ.setLogger(this.logger);
        this.authServ.setEnv(this.env);
        given(this.env.getProperty(anyString())).willReturn(encodedTestsKey);

        this.nonExpiredToken = this.authServ.getNewTokenValidFor(2);
    }

    @Test
    void getNewEncodedSigningKeyReturnsNonBlankBase64EncodedString() {
        String k = JwtAuthenticationService.getNewEncodedSigningKey();

        assertThat(k).isNotBlank();
        assertThat(k).isBase64();
    }

    @Test
    void getNewTokenValidForOneHourReturnsNonEmptyNonExpiredValidToken() {
        String token = this.authServ.getNewTokenValidFor(1);
        String authHeader = BEARER_PREFIX + token;

        assertThat(token).isNotBlank();
        assertThat(this.authServ.isValidToken(authHeader)).isTrue();
    }

    @Test
    void isValidTokenReturnsFalseWhenExpiredJwtExceptionIsThrown() {
        String authHeader = BEARER_PREFIX + this.authServ.getNewTokenValidFor(-1);

        assertThat(this.authServ.isValidToken(authHeader)).isFalse();
    }

    @Test
    void isValidTokenReturnsTrueWhenGetExpirationClaimDoesNotThrowJwtException() {
        String authHeader = BEARER_PREFIX + this.nonExpiredToken;

        assertThat(this.authServ.isValidToken(authHeader)).isTrue();
    }

    @Test
    void isValidTokenReturnsFalseWhenValidTokenIsAltered() {
        String header = BEARER_PREFIX + this.nonExpiredToken;
        int idx = header.length() - 90;

        // Replace a digit with the next one
        Matcher matcher = Pattern.compile("[0-9]").matcher(header);
        // Start looking for a digit in the last characters of the token
        matcher.find(idx);
        int start = matcher.start();
        String h0 = header.substring(0, start)
            .concat(
                // charAt(start) - '0': This converts the character from its ASCII (or Unicode) value to its integer representation.
                // In ASCII, the characters '0' through '9' have consecutive values, with '0' being 48, '1' being 49, and so on.
                // Subtracting '0' (which is 48) from the character's ASCII value gives its numeric value.
                // For example, if the character is '3', charAt(start) - '0' results in 51 - 48, which is 3.
                // Then increments the digit by 1 and takes the result modulo 10 to handle the case where the digit is 9,
                // which is followed by 0.
                String.valueOf((header.charAt(start) - '0' + 1) % 10)
            )
            .concat(header.substring(start + 1));

        // Change a lowercase letter to uppercase
        matcher = Pattern.compile("[a-z]").matcher(header);
        matcher.find(idx);
        start = matcher.start();
        String h1 = header.substring(0, start)
            .concat(String.valueOf(header.charAt(start)).toUpperCase())
            .concat(header.substring(start + 1));

        // Replace first uppercase letter with a lowercase x
        String h2 = BEARER_PREFIX + this.nonExpiredToken.replaceFirst("[A-Z]", "x");

        assertThat(this.authServ.isValidToken(h0)).isFalse();
        assertThat(this.authServ.isValidToken(h1)).isFalse();
        assertThat(this.authServ.isValidToken(h2)).isFalse();
        verify(this.logger, times(3)).warn(anyString(), anyString());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Other header value"})
    void isValidTokenReturnsFalseWhenAuthHeaderIsNullOrDoesNotHaveBearerPrefix(String authHeader) {
        assertThat(this.authServ.isValidToken(authHeader)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Different Issuer"})
    void isValidTokenReturnsFalseWhenDifferentIssuerOrNoIssuer(String issuer) {
        Date expiry = Date.from(Instant.now().plus(Duration.ofHours(1)));
        SecretKey sk = Keys.hmacShaKeyFor(Decoders.BASE64.decode(this.env.getProperty(SECRET_PROPERTY)));

        String tokenA = Jwts.builder()
            .issuer(issuer)
            .issuedAt(new Date())
            .expiration(expiry)
            .signWith(sk)
            .compact();

        String tokenB = Jwts.builder()
            .issuedAt(new Date())
            .expiration(expiry)
            .signWith(sk)
            .compact();

        assertThat(this.authServ.isValidToken(BEARER_PREFIX + tokenA)).isFalse();
        assertThat(this.authServ.isValidToken(BEARER_PREFIX + tokenB)).isFalse();
    }
}
