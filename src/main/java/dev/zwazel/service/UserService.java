package dev.zwazel.service;

import dev.zwazel.domain.User;
import dev.zwazel.repository.RoleRepository;
import dev.zwazel.repository.UserRepository;
import dev.zwazel.security.AuthController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Value("${roles.user}")
    private String userRoleName;

    public Mono<User> register(AuthController.LoginRegisterRequest req) {
        return userRepository                // 1) does that username exist?
            .existsByUsernameIgnoreCase(req.username())
            .flatMap(exists -> {
                if (exists) {
                    // squeal early—reactively!
                    return Mono.error(new IllegalArgumentException("Username already exists"));
                }

                // 2) fetch role… reactively
                return roleRepository.findByNameIgnoreCase(userRoleName)
                    .switchIfEmpty(Mono.error(
                        new IllegalStateException("User role not found")))
                    // 3) build entity
                    .map(role -> User.ofPlainPassword(
                            req.username(), req.password(), Set.of(role)))
                    // 4) save, still non‑blocking
                    .flatMap(userRepository::save);
            });
    }
}
