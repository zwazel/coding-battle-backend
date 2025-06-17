package dev.zwazel.security;

import dev.zwazel.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final UserService userService;
    private final AuthService authService;

    @Value("${jwt.defaultExpiration}")
    private long DEFAULT_TTL;

    /*──────────────────────────────────────────────────*/
    /*  POST /auth/login                                */
    /*──────────────────────────────────────────────────*/
    @PostMapping("/login")
    public Mono<ResponseEntity<Void>> login(
            @RequestBody LoginRegisterRequest req) {

        log.info("LOGIN attempt for {}", req.username());

        Long ttlSeconds = (req.ttlSeconds() == null || req.ttlSeconds() <= 0) ? DEFAULT_TTL : req.ttlSeconds();

        return authService.login(req.username(), req.password(), ttlSeconds)
                .map(c -> {
                    log.info("LOGIN success for {}", req.username());
                    return ResponseEntity.ok()
                            .header(HttpHeaders.SET_COOKIE, c.toString())
                            .build();
                });
    }

    /*──────────────────────────────────────────────────*/
    /*  POST /auth/register                             */
    /*──────────────────────────────────────────────────*/
    @PostMapping("/register")
    public Mono<ResponseEntity<Void>> register(
            @RequestBody LoginRegisterRequest req) {

        log.info("REGISTER attempt for {}", req.username());

        Long ttlSeconds = (req.ttlSeconds() == null || req.ttlSeconds() <= 0) ? DEFAULT_TTL : req.ttlSeconds();

        return userService.register(req)
                .flatMap(user -> {
                    log.info("REGISTER success for {}", req.username());
                    return authService.login(req.username(), req.password(), ttlSeconds)
                            .map(c -> ResponseEntity.ok()
                                    .header(HttpHeaders.SET_COOKIE, c.toString())
                                    .build());
                });
    }

    /*──────────────────────────────────────────────────*/
    /*  DTOs                                            */
    /*──────────────────────────────────────────────────*/

    public record LoginRegisterRequest(String username,
                                       String password,
                                       Long ttlSeconds) {
    }
}
