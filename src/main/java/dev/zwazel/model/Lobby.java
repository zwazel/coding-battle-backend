package dev.zwazel.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.zwazel.domain.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class Lobby {
    private final String name;
    private final List<User> players;

    @JsonIgnore
    private GameState gameState;
}