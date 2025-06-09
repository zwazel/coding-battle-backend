package dev.zwazel.controller;

import dev.zwazel.service.SimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/simulation")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    /**
     * Start the simulation for the given lobby.
     * If it's already started, this does nothing.
     */
    @PostMapping("/{lobbyId}/start")
    public void startSimulation(@PathVariable String lobbyId) {
        simulationService.startSimulation(lobbyId);
    }
}