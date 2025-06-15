package dev.zwazel.repository;

import dev.zwazel.domain.Bot;
import dev.zwazel.domain.User;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface BotRepository extends R2dbcRepository<Bot, UUID> {
    Mono<Boolean> existsByNameIgnoreCaseAndOwner(String nameLower, User user);
}
