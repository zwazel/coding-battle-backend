package dev.zwazel.seed;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
@RequiredArgsConstructor
@DependsOn("generalSeeder")
class DevSeeder implements CommandLineRunner {
    @Override
    @Transactional
    public void run(String... args) throws Exception {

    }
}
