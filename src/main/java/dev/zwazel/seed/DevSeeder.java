package dev.zwazel.seed;

import dev.zwazel.domain.Role;
import dev.zwazel.domain.User;
import dev.zwazel.repository.RoleRepository;
import dev.zwazel.repository.UserRepository;
import jakarta.annotation.Priority;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@Profile("dev")
@RequiredArgsConstructor
@DependsOn("generalSeeder")
@Priority(2)
class DevSeeder implements CommandLineRunner {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Value("${roles.user}")
    private String userRolesName;

    @Value("${dev.user.password}")
    private String testUserPassword;

    @Value("${dev.user.username}")
    private String testUserUsername;

    @Value("${dev.user.count}")
    private int testUserCount;

    @Override
    @Transactional
    public void run(String... args) {
        Role userRole = roleRepository.findByName(userRolesName).orElseThrow(() -> new IllegalStateException("User role not found. Please run the general seeder first."));

        for (int i = 0; i < testUserCount; i++) {
            String username = testUserUsername + "_" + (i + 1);
            if (userRepository.findByUsernameLower(username.toLowerCase()).isPresent()) {
                continue; // Skip if the User already exists
            }
            userRepository.save(User.ofPlainPassword(username, testUserPassword, Set.of(userRole)));
        }
    }
}
