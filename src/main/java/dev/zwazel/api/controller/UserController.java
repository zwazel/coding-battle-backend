package dev.zwazel.api.controller;

import dev.zwazel.api.hal.assembler.UserModelAssembler;
import dev.zwazel.api.hal.model.UserModel;
import dev.zwazel.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository;

    private final UserModelAssembler userModelAssembler;

    @GetMapping("/{id}")
    public Mono<EntityModel<UserModel>> one(@PathVariable UUID id, ServerWebExchange ex) {
        return Mono.fromCallable(() ->
                        userRepository.findById(id)
                                .orElseThrow(() ->
                                        new EntityNotFoundException(id.toString())))
                .subscribeOn(Schedulers.boundedElastic())           // ⬅️ isolate blocking JPA
                .flatMap(user -> userModelAssembler.toModel(user, ex)) // assembler stays reactive
                .map(EntityModel::of);
    }
}
