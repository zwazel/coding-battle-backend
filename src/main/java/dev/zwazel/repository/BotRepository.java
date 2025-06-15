package dev.zwazel.repository;

import dev.zwazel.domain.Bot;
import dev.zwazel.domain.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface BotRepository extends ReactiveCrudRepository<Bot, UUID> {
    Mono<Boolean> existsByNameIgnoreCaseAndOwner(String nameLower, User user);
}
