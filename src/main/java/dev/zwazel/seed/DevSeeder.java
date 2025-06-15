package dev.zwazel.seed;

import dev.zwazel.domain.Bot;
import dev.zwazel.domain.Role;
import dev.zwazel.domain.User;
import dev.zwazel.repository.BotRepository;
import dev.zwazel.repository.RoleRepository;
import dev.zwazel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;

@Component
@Profile("dev")
@DependsOn("generalSeeder")
@Order(2)                          // replaces @Priority for ApplicationRunner
@RequiredArgsConstructor
@Slf4j
public class DevSeeder implements ApplicationRunner {

    private final RoleRepository roleRepo;
    private final UserRepository userRepo;
    private final BotRepository botRepo;

    @Value("${roles.user}")
    private String userRoleName;

    @Value("${dev.user.password}")
    private String pw;

    @Value("${dev.user.username}")
    private String baseUser;

    @Value("${dev.user.count}")
    private int userCount;

    /*─────────────────────────────────────────────────────
     *  File‑system helpers (blocking, called on elastic)
     *────────────────────────────────────────────────────*/
    private static void tryDeleteQuietly(Path dir) {
        if (dir == null) return;
        try (var paths = Files.walk(dir)) {             // ⚡ auto‑close the stream
            paths.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception ignored) {
        }                 // swallow all—no traces!
    }

    /*─────────────────────────────────────────────────────
     *  ApplicationRunner – called at context refresh
     *────────────────────────────────────────────────────*/
    @Override
    public void run(ApplicationArguments args) {
        log.info("DevSeeder: Starting…");

        seedUsers()
                .then(cleanOrphanBots())
                .subscribeOn(Schedulers.boundedElastic())   // ⬅️ hop once
                .doOnSuccess(v -> log.info("DevSeeder: Completed successfully"))
                .doOnError(err -> log.error("DevSeeder: Error during seeding", err))
                .subscribe();
    }


    /*─────────────────────────────────────────────────────
     *  1: Create dev users if missing
     *────────────────────────────────────────────────────*/
    private Mono<Void> seedUsers() {

        Mono<Role> userRole = roleRepo.findByNameIgnoreCase(userRoleName)
                .switchIfEmpty(Mono.error(new IllegalStateException(
                        "User role not found—run general seeder first")));

        return userRole.flatMapMany(role -> Flux.range(0, userCount)
                        .map(i -> "%s_%d".formatted(baseUser, i + 1))
                        .flatMap(username ->
                                userRepo.findByUsernameIgnoreCase(username)
                                        .switchIfEmpty(Mono.defer(() -> userRepo.save(
                                                User.ofPlainPassword(username, pw, Set.of(role)))))
                        ))
                .then();
    }

    /*─────────────────────────────────────────────────────
     *  2: Verify bot folders, purge orphans
     *────────────────────────────────────────────────────*/
    private Mono<Void> cleanOrphanBots() {

        return botRepo.findAll()
                .flatMap(this::validateOrDeleteBot)   // per‑bot validation
                .then();
    }

    /* validate one bot, maybe delete */
    private Mono<Void> validateOrDeleteBot(Bot bot) {

        return Mono.fromCallable(() -> isBotFolderIntact(bot))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(intact -> intact ? Mono.empty() : purgeBot(bot))
                .onErrorResume(err -> purgeBot(bot)
                        .doOnSuccess(v ->
                                log.warn("DevSeeder: Error checking bot {}, purged: {}",
                                        bot.getName(), err.toString())));
    }

    /* blocking file‑system scan */
    private boolean isBotFolderIntact(Bot bot) {

        Path source = Path.of(bot.getSourcePath());
        Path wasm = Path.of(bot.getWasmPath());

        Path root = source.getParent().getParent();
        Path sourceDir = source.getParent();
        Path compiledDir = wasm.getParent();

        return Files.isDirectory(root) &&
                Files.isDirectory(sourceDir) && Files.exists(source) &&
                Files.isDirectory(compiledDir) && Files.exists(wasm);
    }

    /* delete bot DB row + wipe folder */
    private Mono<Void> purgeBot(Bot bot) {

        return botRepo.delete(bot)
                .then(Mono.fromRunnable(() ->
                                tryDeleteQuietly(Path.of(bot.getSourcePath()).getParent().getParent()))
                        .subscribeOn(Schedulers.boundedElastic()))
                .doOnSuccess(v ->
                        log.info("DevSeeder: Deleted bot {} (id={}) due to missing files",
                                bot.getName(), bot.getId()))
                .onErrorResume(err -> {
                    log.warn("DevSeeder: Failed to purge bot {}: {}", bot.getName(), err.toString());
                    return Mono.empty();
                }).then();
    }
}
