package cf.maybelambda.httpvalidator.springboot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static cf.maybelambda.httpvalidator.springboot.service.JwtAuthenticationService.BEARER_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;

public class JwtAuthenticationServiceTests {
    private JwtAuthenticationService authServ;
    private String nonExpiredToken;

    @BeforeEach
    void setUp() {
        String encodedTestsKey = "1Nu1KUNoBSlaOHyH1En88wGDvSM3qV3zdE9tcASGu87JsJf33UjofHGFW42U5ZRypAJz81OWydStVuX+qilW3Q==";
        this.authServ = new JwtAuthenticationService("");
        this.authServ.setSecretKey(encodedTestsKey);

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
    void isExpiredReturnsTrueWhenExpiredJwtExceptionIsThrown() {
        String token = this.authServ.getNewTokenValidFor(-1);

        assertThat(this.authServ.isExpired(token)).isTrue();
    }

    @Test
    void isExpiredReturnsFalseWhenGetExpirationClaimDoesNotThrowJwtException() {
        assertThat(this.authServ.isExpired(this.nonExpiredToken)).isFalse();
    }
}
