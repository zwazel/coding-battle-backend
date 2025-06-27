package dev.zwazel.api.hal.assembler;

import dev.zwazel.api.controller.BotController;
import dev.zwazel.api.controller.UserController;
import dev.zwazel.api.hal.model.BotModel;
import dev.zwazel.domain.Bot;
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
public class BotModelAssembler implements RepresentationModelAssembler<Bot, BotModel> {
    @Override
    public BotModel toModel(Bot bot) {
        BotModel model = new BotModel(
                bot.getId(),
                bot.getName()
        );

        // self
        model.add(linkTo(methodOn(BotController.class)
                .one(bot.getId())).withSelfRel());

        // all bots for this owner
        model.add(linkTo(methodOn(BotController.class)
                .getBotsByUserId(bot.getId())).withRel("allOwnedBots"));

        // Link to the owner of the bot
        model.add(linkTo(methodOn(UserController.class)
                .one(bot.getOwner().getId())).withRel("owner"));

        return model;
    }

    @Override
    public CollectionModel<BotModel> toCollectionModel(Iterable<? extends Bot> bots) {
        return RepresentationModelAssembler.super.toCollectionModel(bots);
    }
}
