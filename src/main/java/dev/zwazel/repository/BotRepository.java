package dev.zwazel.repository;

import dev.zwazel.domain.Bot;
import dev.zwazel.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BotRepository extends JpaRepository<Bot, UUID> {
    boolean existsByNameIgnoreCaseAndOwner(String nameLower, User user);

    Iterable<Bot> findAllByOwner(User user);
}
