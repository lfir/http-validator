package cf.maybelambda.httpvalidator.springboot.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.function.Function;

import static java.util.Objects.nonNull;

@Service
public class JwtAuthenticationService {
    static final String BEARER_PREFIX = "Bearer ";
    private SecretKey key;
    private static Logger logger = LoggerFactory.getLogger(JwtAuthenticationService.class);

    public JwtAuthenticationService(@Value("${jwt.signing.secret}") String encodedSigningKey) {
        try {
            this.setSecretKey(encodedSigningKey);
        } catch (WeakKeyException e) {
            logger.warn("JwtAuthenticationService initialized with EMPTY or WEAK Secret Key");
        }
    }

    static String getNewEncodedSigningKey() {
        return Encoders.BASE64.encode(Jwts.SIG.HS512.key().build().getEncoded());
    }

    String getNewTokenValidFor(int hours) {
        return Jwts.builder()
            .issuedAt(new Date())
            .expiration(Date.from(Instant.now().plus(Duration.ofHours(hours))))
            .signWith(this.key)
            .compact();
    }

    private Jws<Claims> extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(this.key)
            .build()
            .parseSignedClaims(token);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Jws<Claims> claims = this.extractAllClaims(token);
        return claimsResolver.apply(claims.getPayload());
    }

    boolean isExpired(String token) {
        try {
            this.extractClaim(token, Claims::getExpiration);
            return false;
        } catch (ExpiredJwtException e) { return true; }
    }

    public boolean isValidToken(String authorizationHeader) {
        boolean ans = false;
        if (nonNull(authorizationHeader) && authorizationHeader.startsWith(BEARER_PREFIX)) {
            String token = authorizationHeader.replace(BEARER_PREFIX, "");
            ans = !this.isExpired(token);
        }

        return ans;
    }

    void setSecretKey(String encodedSigningKey) { this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(encodedSigningKey)); }
}
