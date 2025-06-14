package dev.zwazel.seed;

import dev.zwazel.domain.Bot;
import dev.zwazel.domain.Role;
import dev.zwazel.domain.User;
import dev.zwazel.repository.BotRepository;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;

@Component
@Profile("dev")
@RequiredArgsConstructor
@DependsOn("generalSeeder")
@Priority(2)
class DevSeeder implements CommandLineRunner {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final BotRepository botRepo;

    @Value("${roles.user}")
    private String userRolesName;

    @Value("${dev.user.password}")
    private String testUserPassword;

    @Value("${dev.user.username}")
    private String testUserUsername;

    @Value("${dev.user.count}")
    private int testUserCount;

    /**
     * Deletes a directory tree in depth‑first order (Files.walk is Java 8+).
     */
    private static void deleteRecursively(Path dir) throws IOException {
        if (dir == null || Files.notExists(dir)) {
            System.out.println("DevSeeder: Directory does not exist or is null: " + dir);
            return; // Nothing to delete
        }

        Files.walk(dir)
                .sorted(Comparator.reverseOrder())   // children before parent
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ex) {
                        // log.warn("Failed to delete {}", path, ex);
                    }
                });
    }

    /**
     * Failsafe: swallow all exceptions—rats leave no traces.
     */
    private static void tryDeleteQuietly(Path dir) {
        try {
            deleteRecursively(dir);
        } catch (IOException ignored) {
        }
    }

    @Override
    @Transactional
    public void run(String... args) {
        Role userRole = roleRepository.findByNameIgnoreCase(userRolesName).orElseThrow(() -> new IllegalStateException("User role not found. Please run the general seeder first."));

        for (int i = 0; i < testUserCount; i++) {
            String username = testUserUsername + "_" + (i + 1);
            if (userRepository.findByUsernameIgnoreCase(username).isPresent()) {
                continue; // Skip if the User already exists
            }
            userRepository.save(User.ofPlainPassword(username, testUserPassword, Set.of(userRole)));
        }

        /* at startup check the Bots table, and check if that folder still exists, if not, delete the Bot from the DB */
        /* Check if the general folder exists of the bot, and then if source and compiled exists */
        for (Bot bot : botRepo.findAll()) {
            try {
                Path sourceFile = Path.of(bot.getSourcePath());
                Path wasmFile = Path.of(bot.getWasmPath());

                Path botDir = sourceFile.getParent().getParent(); // root/<botName>/
                Path sourceDir = sourceFile.getParent();
                Path compiledDir = wasmFile.getParent();

                boolean sourceOk = Files.isDirectory(sourceDir) && Files.exists(sourceFile);
                boolean compiledOk = Files.isDirectory(compiledDir) && Files.exists(wasmFile);
                boolean rootOk = Files.isDirectory(botDir);

                if (!(rootOk && sourceOk && compiledOk)) {
                    // 1) Remove DB entry (same as before)
                    botRepo.delete(bot);

                    // 2) Attempt to wipe the entire bot folder tree
                    deleteRecursively(botDir);

                    System.out.println("DevSeeder: Deleted bot " + bot.getName() + " due to missing files.");
                }
            } catch (Exception e) {              // any error?  Purge it all!
                botRepo.delete(bot);
                tryDeleteQuietly(Path.of(bot.getSourcePath()).getParent().getParent());

                System.out.println("DevSeeder: Error while checking bot " + bot.getName() + ", deleted it: " + e.getMessage());
            }
        }
    }
}
