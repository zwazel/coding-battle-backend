package dev.zwazel.security;

import dev.zwazel.domain.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class JwtUtil {
    @Value("${jwt.secret}")
    private String jwtSecret;

    Mono<String> generate(User u, Long ttlSeconds) {
        Instant now = Instant.now();
        return Mono.fromCallable(() ->
                Jwts.builder()
                        .subject(u.getUsername())
                        .claim("roles", u.getRoles().stream()
                                .map(role -> role.getName().toUpperCase())
                                .toList())
                        .issuedAt(Date.from(now))
                        .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                        .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                        .compact()
        );
    }

    Mono<Authentication> validate(String token) {
        log.debug("JWT token validation started");
        return Mono.fromCallable(() -> {
                    log.debug("Parsing JWT token");
                    return Jwts.parser()
                            .verifyWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                            .build()
                            .parseSignedClaims(token);
                })
                .map(c -> {
                    log.debug("JWT token parsed successfully, subject: {}", c.getPayload().getSubject());
                    List<?> roles = c.getPayload().get("roles", List.class);
                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(role -> "ROLE_" + role.toString().toUpperCase())
                            .map(SimpleGrantedAuthority::new)
                            .toList();
                    log.debug("Extracted authorities: {}", authorities);
                    return new UsernamePasswordAuthenticationToken(
                            c.getPayload().getSubject(),
                            null,
                            authorities
                    );
                });
    }
}
