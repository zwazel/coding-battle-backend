package dev.zwazel.service;

import dev.zwazel.domain.User;
import dev.zwazel.repository.RoleRepository;
import dev.zwazel.repository.UserRepository;
import dev.zwazel.security.AuthController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Value("${roles.user}")
    private String userRoleName;

    public User register(AuthController.LoginRegisterRequest req) {
        if (userRepository.existsByUsernameLower((req.username().toLowerCase()))) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = User.ofPlainPassword(req.username(), req.password(),
                Set.of(roleRepository.findByName(userRoleName).orElseThrow(() -> new IllegalStateException("User role not found"))));
        return userRepository.save(user);
    }
}
