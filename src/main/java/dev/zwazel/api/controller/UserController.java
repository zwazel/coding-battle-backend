package dev.zwazel.api.controller;

import dev.zwazel.api.hal.assembler.UserModelAssembler;
import dev.zwazel.api.hal.model.UserModel;
import dev.zwazel.domain.User;
import dev.zwazel.exception.UserNotFoundException;
import dev.zwazel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository;

    private final UserModelAssembler userModelAssembler;

    @GetMapping("/{id}")
    public EntityModel<UserModel> one(@PathVariable UUID id) {
        log.info("one called with id: {}", id);
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));

        return EntityModel.of(
                userModelAssembler.toModel(user)
        );
    }

    @GetMapping
    public CollectionModel<UserModel> all() {
        log.info("all called");
        return userModelAssembler.toCollectionModel(userRepository.findAll());
    }
}
