package dev.zwazel.seed;

import dev.zwazel.domain.Role;
import dev.zwazel.domain.User;
import dev.zwazel.repository.RoleRepository;
import dev.zwazel.repository.UserRepository;
import jakarta.annotation.Priority;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

@Component
@RequiredArgsConstructor
@DependsOnDatabaseInitialization
@Priority(1)
class GeneralSeeder implements CommandLineRunner {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${roles.admin}")
    private String adminRoleName;

    @Value("${roles.user}")
    private String userRolesName;

    @Override
    @Transactional
    public void run(String... args) throws IOException {
        Files.createDirectories(Path.of("artifacts"));

        Role adminRole;
        if (!roleRepository.existsByName(adminRoleName)) {
            adminRole = roleRepository.save(Role.builder().name(adminRoleName).build());
        } else {
            adminRole = roleRepository.findByName(adminRoleName);
        }

        Role userRole;
        if (!roleRepository.existsByName(userRolesName)) {
            userRole = roleRepository.save(Role.builder().name(userRolesName).build());
        } else {
            userRole = roleRepository.findByName(userRolesName);
        }

        userRepository.save(User.ofPlainPassword(adminUsername, adminPassword, Set.of(adminRole, userRole)));
    }
}
