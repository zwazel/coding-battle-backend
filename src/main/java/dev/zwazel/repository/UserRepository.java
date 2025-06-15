package dev.zwazel.repository;

import dev.zwazel.domain.User;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface UserRepository extends R2dbcRepository<User, UUID> {
    Mono<User> findByUsernameIgnoreCase(String username);

    Mono<Boolean> existsByUsernameIgnoreCase(String username);
}
