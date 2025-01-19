package dev.zwazel.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

@RestController
public class SimulationController {

    @GetMapping(value = "/startSimulation", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> startSimulation() {
        // This simulates “Hello World” messages being sent every second.
        return Flux.interval(Duration.ofSeconds(1))
                   .map(count -> "Hello World! Count = " + count);
    }
}
