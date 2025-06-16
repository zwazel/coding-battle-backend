package dev.zwazel.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
@Slf4j
class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtService jwt;
    private final ReactiveUserDetailsService users;

    @Override
    public Mono<Authentication> authenticate(Authentication auth) {
        if (auth == null || auth.getCredentials() == null) {
            log.warn("Authentication attempt with null credentials");
            return Mono.empty();
        }

        String token = (String) auth.getCredentials();
        if (token.isBlank()) {
            log.warn("Authentication attempt with empty token");
            return Mono.empty();                  // empty token → let it pass as anonymous
        }

        log.info("Authenticating with token: {}", token);

        return Mono.fromCallable(() -> jwt.parse(token))
                .flatMap(jws -> users.findByUsername(jws.getPayload().getSubject()))
                .map(u -> (Authentication) new UsernamePasswordAuthenticationToken(
                        u, token, u.getAuthorities()))
                .doOnNext(a -> log.info("Authenticated user: {}", a.getName()))
                .doOnError(e -> log.error("Authentication failed", e))
                .switchIfEmpty(Mono.error(new BadCredentialsException("Invalid token: " + token)));
    }
}
