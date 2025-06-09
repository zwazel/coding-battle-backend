package dev.zwazel.seed;

import dev.zwazel.domain.Role;
import dev.zwazel.domain.User;
import dev.zwazel.repository.RoleRepository;
import dev.zwazel.repository.UserRepository;
import jakarta.annotation.Priority;
import lombok.RequiredArgsConstructor;
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

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Get User Roles and create 2 users with those roles
        Role userRole = roleRepository.findByName("USER");

        if (userRole == null) {
            throw new IllegalStateException("User role not found. Please run the general seeder first.");
        }

        // Create a user with the USER role
        userRepository.save(User.ofPlainPassword("devUser1", "DevUser123", Set.of(userRole)));
        userRepository.save(User.ofPlainPassword("devUser2", "DevUser123", Set.of(userRole)));
    }
}
