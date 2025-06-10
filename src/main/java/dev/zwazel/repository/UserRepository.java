package dev.zwazel.repository;

import dev.zwazel.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsernameLower(String username);

    boolean existsByUsernameLower(String username);
}
