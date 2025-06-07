package dev.zwazel.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class Lobby {
    private final String id;
    private final List<Player> players;

    @JsonIgnore
    private GameState gameState;
}