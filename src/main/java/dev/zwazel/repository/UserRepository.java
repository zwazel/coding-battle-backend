package dev.zwazel.repository;

import dev.zwazel.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends ReactiveCrudRepository<User, UUID> {
    Mono<User> findByUsernameIgnoreCase(String username);

    Mono<Boolean> existsByUsernameIgnoreCase(String username);
}
