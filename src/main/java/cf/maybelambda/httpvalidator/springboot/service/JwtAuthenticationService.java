package cf.maybelambda.httpvalidator.springboot.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static java.util.Objects.nonNull;

@Service
public class JwtAuthenticationService {
    public static final String BEARER_PREFIX = "Bearer ";
    static final String SECRET_PROPERTY = "jwt.signing.secret";
    @Autowired
    private Environment env;

    static String getNewEncodedSigningKey() {
        return Encoders.BASE64.encode(Jwts.SIG.HS512.key().build().getEncoded());
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(this.env.getProperty(SECRET_PROPERTY)));
    }

    public String getNewTokenValidFor(int hours) {
        return Jwts.builder()
            .issuedAt(new Date())
            .expiration(Date.from(Instant.now().plus(Duration.ofHours(hours))))
            .signWith(this.getSecretKey())
            .compact();
    }

    private Jws<Claims> extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(this.getSecretKey())
            .build()
            .parseSignedClaims(token);
    }

    public boolean isValidToken(String authorizationHeader) {
        boolean ans = false;
        if (nonNull(authorizationHeader) && authorizationHeader.startsWith(BEARER_PREFIX)) {
            String token = authorizationHeader.replace(BEARER_PREFIX, "");
            try {
                Jws<Claims> claims = this.extractAllClaims(token);
                ans = true;
            } catch (JwtException ignored) {}
        }

        return ans;
    }

    void setEnv(Environment env) { this.env = env; }
}
