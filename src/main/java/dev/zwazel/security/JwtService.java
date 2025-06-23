package dev.zwazel.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
class JwtService {
    private final SecretKey key;

    public String generate(UserDetails user, Duration ttl) {
        log.info("Generating JWT for user: {}", user.getUsername());
        Instant now = Instant.now();
        String token = Jwts.builder()
                .subject(user.getUsername())
                .claim("roles", user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        log.info("Successfully generated JWT for user: {}", user.getUsername());
        return token;
    }

    public Jws<Claims> parse(String token) {
        log.debug("Parsing JWT token");
        try {
            Jws<Claims> claimsJws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            log.debug("Successfully parsed JWT token for user: {}", claimsJws.getPayload().getSubject());
            return claimsJws;
        } catch (Exception e) {
            log.warn("Failed to parse JWT token", e);
            throw e;
        }
    }
}
