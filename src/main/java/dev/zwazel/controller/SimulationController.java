package dev.zwazel.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/simulation")
public class SimulationController {

    // Map of lobby IDs to their shared flux
    private final Map<String, Flux<String>> lobbyFluxes = new ConcurrentHashMap<>();

     @GetMapping(value = "/{lobbyId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamLobby(@PathVariable String lobbyId) {
        // Return existing flux if present, otherwise create and store a new one
        return lobbyFluxes.computeIfAbsent(lobbyId, id -> createSharedFluxForLobby());
    }

    private Flux<String> createSharedFluxForLobby() {
        return Flux.interval(Duration.ofSeconds(1))
                   .map(count -> "Lobby event " + count)
                   .share();
    }
}
