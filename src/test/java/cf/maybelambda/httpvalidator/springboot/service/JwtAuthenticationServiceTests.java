package cf.maybelambda.httpvalidator.springboot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.env.Environment;

import static cf.maybelambda.httpvalidator.springboot.service.JwtAuthenticationService.BEARER_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class JwtAuthenticationServiceTests {
    private JwtAuthenticationService authServ;
    private String nonExpiredToken;
    private final Environment env = mock(Environment.class);

    @BeforeEach
    void setUp() {
        String encodedTestsKey = "1Nu1KUNoBSlaOHyH1En88wGDvSM3qV3zdE9tcASGu87JsJf33UjofHGFW42U5ZRypAJz81OWydStVuX+qilW3Q==";
        this.authServ = new JwtAuthenticationService();

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
    void getNewTokenValidForReturnsNonEmptyNonExpiredValidToken() {
        String token = this.authServ.getNewTokenValidFor(1);

        assertThat(token).isNotBlank();
        assertThat(this.authServ.isValidToken(BEARER_PREFIX + token)).isTrue();
    }

    @Test
    void isValidTokenReturnsFalseWhenExpiredJwtExceptionIsThrown() {
        String authHeader = BEARER_PREFIX + this.authServ.getNewTokenValidFor(-1);

        assertThat(this.authServ.isValidToken(authHeader)).isFalse();
    }

    @Test
    void isValidTokenReturnsTrueWhenGetExpirationClaimDoesNotThrowJwtException() {
        assertThat(this.authServ.isValidToken(BEARER_PREFIX + this.nonExpiredToken)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Other header value"})
    void isValidTokenReturnsFalseWhenAuthHeaderIsNullOrDoesNotHaveBearerPrefix(String authHeader) {
        assertThat(this.authServ.isValidToken(authHeader)).isFalse();
    }
}
