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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

@Component
@RequiredArgsConstructor
@DependsOnDatabaseInitialization
@Priority(1)
class GeneralSeeder implements CommandLineRunner {
    private final RoleRepository roleRepo;
    private final UserRepository userRepo;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${roles.admin}")
    private String adminRoleName;

    @Value("${roles.user}")
    private String userRoleName;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        // make sure artifacts/ exists
        Files.createDirectories(Path.of("artifacts"));

        /* ---------- ensure roles exist (create-if-absent) ------------ */
        Role adminRole = roleRepo.findByNameIgnoreCase(adminRoleName)
                .orElseGet(() -> roleRepo.save(Role.builder().name(adminRoleName).build()));

        Role userRole = roleRepo.findByNameIgnoreCase(userRoleName)
                .orElseGet(() -> roleRepo.save(Role.builder().name(userRoleName).build()));

        /* ---------- create admin user only if missing ---------------- */
        if (userRepo.findByUsernameIgnoreCase(adminUsername).isEmpty()) {
            userRepo.save(User.ofPlainPassword(adminUsername,
                    adminPassword,
                    Set.of(adminRole, userRole)));
        }
    }
}
