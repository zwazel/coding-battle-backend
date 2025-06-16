package dev.zwazel.repository;

import dev.zwazel.domain.Bot;
import dev.zwazel.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BotRepository extends JpaRepository<Bot, UUID> {
    boolean existsByNameIgnoreCaseAndOwner(String nameLower, User user);
}
