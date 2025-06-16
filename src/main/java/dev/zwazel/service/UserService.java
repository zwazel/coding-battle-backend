package dev.zwazel.service;

import dev.zwazel.domain.Role;
import dev.zwazel.domain.User;
import dev.zwazel.repository.RoleRepository;
import dev.zwazel.repository.UserRepository;
import dev.zwazel.security.AuthController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
        return Mono.fromCallable(() ->
                {
                    // Blocking
                    if (userRepository.existsByUsernameIgnoreCase(req.username())) {
                        throw new IllegalArgumentException("Username already exists");
                    }

                    // Blocking
                    Role role = roleRepository.findByNameIgnoreCase(userRoleName)
                            .orElseThrow(() -> new IllegalStateException("User role not found"));

                    User newUser = User.ofPlainPassword(req.username(), req.password(), Set.of(role));

                    // Blocking
                    return userRepository.save(newUser);
                }
        ).subscribeOn(Schedulers.boundedElastic()); // shift to thread pool for blocking calls!;
    }
}
