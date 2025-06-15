package dev.zwazel.repository;

import dev.zwazel.domain.Role;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface RoleRepository extends R2dbcRepository<Role, UUID> {
    Mono<Role> findByNameIgnoreCase(String role);

    Mono<Boolean> existsByNameIgnoreCase(String roleName);
}
