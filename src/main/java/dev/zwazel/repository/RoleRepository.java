package dev.zwazel.repository;

import dev.zwazel.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByNameIgnoreCase(String role);

    boolean existsByNameIgnoreCase(String roleName);
}
