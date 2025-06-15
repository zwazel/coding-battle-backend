package dev.zwazel.api.hal.assembler;

import dev.zwazel.api.controller.UserController;
import dev.zwazel.api.hal.model.UserModel;
import dev.zwazel.domain.User;
import dev.zwazel.repository.BotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
@RequiredArgsConstructor
public class UserModelAssembler implements RepresentationModelAssembler<User, UserModel> {
    private final BotRepository botRepository;

    @Override
    public UserModel toModel(User user) {
        UserModel model = new UserModel(
                user.getId(),
                user.getUsername()
        );

        // self
        model.add(linkTo(methodOn(UserController.class)
                .one(user.getId())).withSelfRel());

        return model;
    }
}
