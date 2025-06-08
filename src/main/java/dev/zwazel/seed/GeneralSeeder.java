package dev.zwazel.seed;

import dev.zwazel.domain.Role;
import dev.zwazel.domain.User;
import dev.zwazel.repository.RoleRepository;
import dev.zwazel.repository.UserRepository;
import jakarta.annotation.Priority;
import lombok.RequiredArgsConstructor;
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

    @Override
    @Transactional
    public void run(String... args) throws IOException {
        Files.createDirectories(Path.of("artifacts"));

        Role admin = roleRepository.save(Role.builder().name("ADMIN").build());
        Role user = roleRepository.save(Role.builder().name("USER").build());

        userRepository.save(User.ofPlainPassword("admin", System.getProperty("admin.password", "Admin123"), Set.of(admin, user)));
    }
}
