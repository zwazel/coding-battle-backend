package dev.zwazel.DTO;

import java.util.List;

public record CreateLobbyRequestDTO(
        String lobbyname,
        List<PublicUserDTO> players
) {
}
