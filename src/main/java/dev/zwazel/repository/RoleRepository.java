package dev.zwazel.repository;

import dev.zwazel.domain.Role;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface RoleRepository extends ReactiveCrudRepository<Role, UUID> {
    Mono<Role> findByNameIgnoreCase(String role);

    Mono<Boolean> existsByNameIgnoreCase(String roleName);
}
