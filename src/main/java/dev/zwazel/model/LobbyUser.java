package dev.zwazel.model;

import lombok.Builder;

import java.util.UUID;

@Builder
public record LobbyUser(
        UUID userId,
        String username,
        boolean isHost,
        UUID selectedBotId
) {
}
