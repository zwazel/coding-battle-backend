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

@Component("generalSeeder")
@DependsOnDatabaseInitialization
@Order(1)                                     // run before DevSeeder (@Order 2)
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

    /*─────────────────────────────────────
     *  ApplicationRunner entry‑point
     *────────────────────────────────────*/
    @Override
    public void run(ApplicationArguments args) {

        log.info("GeneralSeeder: Bootstrapping roles & admin user…");

        Mono<Void> pipeline =
                ensureArtifactsDir()
                        .then(ensureRolesAndAdminUser())
                        .doOnSuccess(v -> log.info("GeneralSeeder: Completed successfully"))
                        .doOnError(err -> log.error("GeneralSeeder: Seeding failed", err))
                        .subscribeOn(Schedulers.boundedElastic());

        /*  🔑  BLOCK so DevSeeder waits until this finishes */
        pipeline.block();
    }

    /*─────────────────────────────────────
     *  Ensure ./artifacts directory exists
     *────────────────────────────────────*/
    private Mono<Void> ensureArtifactsDir() {
        return Mono.fromCallable(() -> {
                    Files.createDirectories(Path.of("artifacts"));
                    return (Void) null;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    /*─────────────────────────────────────
     *  Ensure roles exist and admin user exists
     *────────────────────────────────────*/
    private Mono<Void> ensureRolesAndAdminUser() {

        /* blocking role handling */
        Mono<Role> adminRoleMono =
                Mono.fromCallable(() ->
                                roleRepo.findByNameIgnoreCase(adminRoleName)
                                        .orElseGet(() ->
                                                roleRepo.save(Role.builder().name(adminRoleName).build())))
                        .subscribeOn(Schedulers.boundedElastic());

        Mono<Role> userRoleMono =
                Mono.fromCallable(() ->
                                roleRepo.findByNameIgnoreCase(userRoleName)
                                        .orElseGet(() ->
                                                roleRepo.save(Role.builder().name(userRoleName).build())))
                        .subscribeOn(Schedulers.boundedElastic());

        /* create admin user if missing */
        return Mono.zip(adminRoleMono, userRoleMono)
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
    }
}
