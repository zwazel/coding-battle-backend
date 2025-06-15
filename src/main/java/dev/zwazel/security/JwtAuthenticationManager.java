package dev.zwazel.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtService jwt;
    private final ReactiveUserDetailsService users;

    @Override
    public Mono<Authentication> authenticate(Authentication auth) {
        String token = (String) auth.getCredentials();

        return Mono.fromCallable(() -> jwt.parse(token))
                .flatMap(jws -> users.findByUsername(jws.getPayload().getSubject()))
                .map(u -> (Authentication) new UsernamePasswordAuthenticationToken(
                        u, token, u.getAuthorities()))
                .onErrorResume(e -> Mono.empty());
    }
}
