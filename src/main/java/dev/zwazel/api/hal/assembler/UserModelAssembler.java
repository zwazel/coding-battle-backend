package dev.zwazel.api.hal.assembler;

import dev.zwazel.api.controller.BotController;
import dev.zwazel.api.controller.UserController;
import dev.zwazel.api.hal.model.UserModel;
import dev.zwazel.domain.User;
import io.micrometer.common.lang.NonNullApi;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
@RequiredArgsConstructor
@NonNullApi
public class UserModelAssembler implements RepresentationModelAssembler<User, UserModel> {
    @Override
    public UserModel toModel(User user) {
        UserModel model = new UserModel(
                user.getId(),
                user.getUsername()
        );

        // self
        model.add(linkTo(methodOn(UserController.class)
                .one(user.getId())).withSelfRel());

        // bots
        // Get all bots for this user
        model.add(linkTo(methodOn(BotController.class)
                .getBotsByUserId(user.getId())).withRel("bots"));

        return model;
    }

    @Override
    public CollectionModel<UserModel> toCollectionModel(Iterable<? extends User> users) {
        CollectionModel<UserModel> collectionModel = RepresentationModelAssembler.super.toCollectionModel(users);
        collectionModel.add(linkTo(methodOn(UserController.class).all()).withSelfRel());
        return collectionModel;
    }
}
