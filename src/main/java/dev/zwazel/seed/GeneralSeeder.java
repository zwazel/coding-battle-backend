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
@Order(1)                       // same priority as before
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

    /*─────────────────────────────────────────────────────
     *  ApplicationRunner – kicked at context refresh
     *────────────────────────────────────────────────────*/
    @Override
    public void run(ApplicationArguments args) {

        log.info("GeneralSeeder: Bootstrapping roles & admin user…");

        /* create artifacts/ directory on elastic pool */
        Mono<Void> ensureArtifactsDir =
                Mono.fromCallable(() -> {
                            Files.createDirectories(Path.of("artifacts"));
                            return true;                            // dummy value
                        })
                        .subscribeOn(Schedulers.boundedElastic())
                        .then();                                        // convert to Mono<Void>

        /* ensure admin & user roles exist */
        Mono<Role> adminRoleMono = roleRepo.findByNameIgnoreCase(adminRoleName)
                .switchIfEmpty(roleRepo.save(Role.builder().name(adminRoleName).build()));

        Mono<Role> userRoleMono = roleRepo.findByNameIgnoreCase(userRoleName)
                .switchIfEmpty(roleRepo.save(Role.builder().name(userRoleName).build()));

        /* create admin user if missing */
        Mono<Void> ensureAdminUser = Mono.zip(adminRoleMono, userRoleMono)
                .flatMap(tuple -> {
                    Role adminRole = tuple.getT1();
                    Role userRole = tuple.getT2();
                    return userRepo.findByUsernameIgnoreCase(adminUsername)
                            .switchIfEmpty(userRepo.save(User.ofPlainPassword(
                                    adminUsername, adminPassword, Set.of(adminRole, userRole))));
                })
                .then();

        /* chain dir‑creation → role seeding → admin user */
        ensureArtifactsDir
                .then(ensureAdminUser)
                .subscribeOn(Schedulers.boundedElastic())     // run whole chain off main thread
                .doOnSuccess(v -> log.info("GeneralSeeder: Completed successfully"))
                .doOnError(err -> log.error("GeneralSeeder: Seeding failed", err))
                .subscribe();                                 // fire‑and‑forget
    }
}
