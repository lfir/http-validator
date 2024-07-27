package cf.maybelambda.httpvalidator.springboot.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static java.util.Objects.nonNull;

/**
 * Service for handling JWT authentication, including token generation,
 * validation, and secret key management.
 */
@Service
public class JwtAuthenticationService {
    public static final String BEARER_PREFIX = "Bearer ";
    static final String SECRET_PROPERTY = "jwt.signing.secret";
    static Logger logger = LoggerFactory.getLogger(JwtAuthenticationService.class);

    @Autowired
    private Environment env;

    /**
     * Generates a new encoded signing key.
     *
     * @return Base64-encoded signing key
     */
    static String getNewEncodedSigningKey() {
        return Encoders.BASE64.encode(Jwts.SIG.HS512.key().build().getEncoded());
    }

    /**
     * Retrieves the Base64-encoded secret key from the environment properties.
     *
     * @return Secret key for signing JWT
     */
    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(this.env.getProperty(SECRET_PROPERTY)));
    }

    /**
     * Generates a new signed JWT token that is valid for a specified number of hours.
     *
     * @param hours Number of hours the token is valid for
     * @return JWT token
     */
    public String getNewTokenValidFor(int hours) {
        return Jwts.builder()
            .issuer(this.getClass().getCanonicalName())
            .issuedAt(new Date())
            .expiration(Date.from(Instant.now().plus(Duration.ofHours(hours))))
            .signWith(this.getSecretKey())
            .compact();
    }

    /**
     * Extracts all claims from a JWT token.
     *
     * @param token JWT token
     * @return Jws containing the claims
     */
    private Jws<Claims> extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(this.getSecretKey())
            .build()
            .parseSignedClaims(token);
    }

    /**
     * Validates a JWT token from the Authorization header of an incoming request.
     *
     * @param authorizationHeader Authorization header containing the JWT token
     * @return true if the token is valid, false otherwise or if the header is null or empty
     */
    public boolean isValidToken(String authorizationHeader) {
        boolean ans = false;
        if (nonNull(authorizationHeader) && authorizationHeader.startsWith(BEARER_PREFIX)) {
            String token = authorizationHeader.replace(BEARER_PREFIX, "");
            try {
                Jws<Claims> claims = this.extractAllClaims(token);
                ans = this.getClass().getCanonicalName().equals(claims.getPayload().getIssuer());
            } catch (JwtException e) {
                logger.warn("Invalid JWT received: {}", e.getMessage());
            }
        }

        return ans;
    }

    /**
     * Sets the environment. Used for testing purposes.
     *
     * @param env Environment
     */
    void setEnv(Environment env) { this.env = env; }

    /**
     * Sets the logger; used for testing purposes.
     *
     * @param logger The logger to set.
     */
    void setLogger(Logger logger) { JwtAuthenticationService.logger = logger; }
}
