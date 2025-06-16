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
import java.util.stream.IntStream;

@Component
@Profile("dev")
@DependsOn("generalSeeder")
@Order(2)
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

    /*─────────────────────────────
     *  Helper – silent folder delete
     *────────────────────────────*/
    private static void tryDeleteQuietly(Path dir) {
        if (dir == null) return;
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception ignored) {
        }
    }

    /*─────────────────────────────
     *  ApplicationRunner
     *────────────────────────────*/
    @Override
    public void run(ApplicationArguments args) {
        log.info("DevSeeder: Starting…");

        seedUsers()
                .then(cleanOrphanBots())
                .subscribeOn(Schedulers.boundedElastic())      // all blocking work off event loop
                .doOnSuccess(v -> log.info("DevSeeder: Completed successfully"))
                .doOnError(err -> log.error("DevSeeder: Error during seeding", err))
                .subscribe();
    }

    /*─────────────────────────────
     *  1️⃣  Create dev users
     *────────────────────────────*/
    private Mono<Void> seedUsers() {
        return Mono.fromCallable(() -> {
                    Role role = roleRepo.findByNameIgnoreCase(userRoleName)
                            .orElseThrow(() -> new IllegalStateException(
                                    "User role not found—run general seeder first"));

                    IntStream.range(0, userCount).forEach(i -> {
                        String username = "%s_%d".formatted(baseUser, i + 1);
                        userRepo.findByUsernameIgnoreCase(username)
                                .or(() -> {
                                    userRepo.save(
                                            User.ofPlainPassword(username, pw, Set.of(role)));
                                    return java.util.Optional.empty();
                                });
                    });
                    return (Void) null;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /*─────────────────────────────
     *  2️⃣  Purge orphan bots
     *────────────────────────────*/
    private Mono<Void> cleanOrphanBots() {
        return Mono.fromCallable(botRepo::findAll)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable)
                .flatMap(this::validateOrDeleteBot)
                .then();
    }

    private Mono<Void> validateOrDeleteBot(Bot bot) {
        return Mono.fromCallable(() -> isBotFolderIntact(bot))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(intact -> intact ? Mono.empty() : purgeBot(bot))
                .onErrorResume(err -> purgeBot(bot)
                        .doOnSuccess(v ->
                                log.warn("DevSeeder: Error checking bot {}, purged: {}",
                                        bot.getName(), err.toString())));
    }

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

    private Mono<Void> purgeBot(Bot bot) {
        return Mono.fromRunnable(() -> botRepo.delete(bot))
                .subscribeOn(Schedulers.boundedElastic())
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
