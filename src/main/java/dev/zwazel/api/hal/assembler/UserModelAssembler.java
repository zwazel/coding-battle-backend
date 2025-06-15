package dev.zwazel.api.hal.assembler;

import dev.zwazel.api.controller.UserController;
import dev.zwazel.api.hal.model.UserModel;
import dev.zwazel.domain.User;
import dev.zwazel.repository.BotRepository;
import io.micrometer.common.lang.NonNullApi;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.reactive.ReactiveRepresentationModelAssembler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.springframework.hateoas.server.reactive.WebFluxLinkBuilder.linkTo;
import static org.springframework.hateoas.server.reactive.WebFluxLinkBuilder.methodOn;

@Component
@RequiredArgsConstructor
@NonNullApi
public class UserModelAssembler implements ReactiveRepresentationModelAssembler<User, UserModel> {
    private final BotRepository botRepository;

    @Override
    public Mono<UserModel> toModel(User user, ServerWebExchange ex) {

        UserModel model = new UserModel(user.getId(), user.getUsername());

        /* build link with request context → returns Mono<Link> */
        Mono<Link> selfLink =
                linkTo(
                        methodOn(UserController.class)
                                .one(user.getId(), ex),
                        ex)
                        .withSelfRel().toMono();

        /* add link once it’s ready */
        return selfLink.doOnNext(model::add).thenReturn(model);
    }
}
