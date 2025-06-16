package dev.zwazel.seed;

import dev.zwazel.domain.Role;
import dev.zwazel.domain.User;
import dev.zwazel.repository.RoleRepository;
import dev.zwazel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

@Component
@DependsOnDatabaseInitialization
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class GeneralSeeder implements ApplicationRunner {

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

    /*─────────────────────────────
     *  ApplicationRunner
     *────────────────────────────*/
    @Override
    public void run(ApplicationArguments args) {

        log.info("GeneralSeeder: Bootstrapping roles & admin user…");

        /* 1️⃣  Ensure ./artifacts directory exists (disk IO) */
        Mono<Void> ensureArtifactsDir =
                Mono.fromCallable(() -> {
                            Files.createDirectories(Path.of("artifacts"));
                            return (Void) null;
                        })
                        .subscribeOn(Schedulers.boundedElastic());

        /* 2️⃣  Ensure roles exist (blocking JPA) */
        Mono<Role> adminRoleMono =
                Mono.fromCallable(() ->
                                roleRepo.findByNameIgnoreCase(adminRoleName)
                                        .orElseGet(() -> roleRepo.save(Role.builder()
                                                .name(adminRoleName).build())))
                        .subscribeOn(Schedulers.boundedElastic());

        Mono<Role> userRoleMono =
                Mono.fromCallable(() ->
                                roleRepo.findByNameIgnoreCase(userRoleName)
                                        .orElseGet(() -> roleRepo.save(Role.builder()
                                                .name(userRoleName).build())))
                        .subscribeOn(Schedulers.boundedElastic());

        /* 3️⃣  Ensure admin user exists (blocking JPA) */
        Mono<Void> ensureAdminUser =
                Mono.zip(adminRoleMono, userRoleMono)
                        .flatMap(tuple -> Mono.fromCallable(() -> {
                                    Role adminRole = tuple.getT1();
                                    Role userRole = tuple.getT2();

                                    userRepo.findByUsernameIgnoreCase(adminUsername)
                                            .or(() -> {
                                                userRepo.save(User.ofPlainPassword(
                                                        adminUsername,
                                                        adminPassword,
                                                        Set.of(adminRole, userRole)));
                                                return java.util.Optional.empty();
                                            });
                                    return (Void) null;
                                })
                                .subscribeOn(Schedulers.boundedElastic()));

        /* 4️⃣  Chain everything and run off the event loop */
        ensureArtifactsDir
                .then(ensureAdminUser)
                .doOnSuccess(v -> log.info("GeneralSeeder: Completed successfully"))
                .doOnError(err -> log.error("GeneralSeeder: Seeding failed", err))
                .subscribe();           // fire‑and‑forget
    }
}
