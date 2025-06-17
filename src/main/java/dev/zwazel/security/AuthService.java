package dev.zwazel.security;

import dev.zwazel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${jwt.cookie.name}")
    private String jwtCookieName;

    public Mono<ResponseCookie> login(String username, String password, Long ttlSeconds) {
        return Mono.fromCallable(() -> userRepository.findByUsernameIgnoreCase(username))
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .flatMap(optionalUser -> optionalUser
                        .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                        .map(user -> jwtUtil.generate(user, ttlSeconds)
                                .map(token -> ResponseCookie.from(jwtCookieName, token)
                                        .httpOnly(true)
                                        .secure(true)
                                        .path("/")
                                        .maxAge(ttlSeconds)
                                        .build()))
                        .orElseGet(Mono::empty)
                );
    }
}
