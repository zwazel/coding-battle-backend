package dev.zwazel.security;

import dev.zwazel.domain.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String jwtSecret;

    Mono<String> generate(User u, Long ttlSeconds) {
        Instant now = Instant.now();
        return Mono.fromCallable(() ->
                Jwts.builder()
                        .subject(u.getUsername())
                        .claim("roles", u.getRoles())
                        .issuedAt(Date.from(now))
                        .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                        .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                        .compact()
        );
    }

    Mono<Authentication> validate(String token) {
        return Mono.fromCallable(() -> Jwts.parser()
                        .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                        .build()
                        .parseSignedClaims(token))
                .map(c ->
                        // c is Jws<Claims>
                        new UsernamePasswordAuthenticationToken(
                                c.getPayload().getSubject(),
                                null,
                                List.copyOf(c.getPayload().get("roles", List.class))
                        )
                );
    }
}
