package dev.zwazel.security;

import dev.zwazel.api.hal.model.LoginResponseModel;
import dev.zwazel.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final ReactiveAuthenticationManager authManager;
    private final JwtService jwt;
    private final UserService userService;

    @Value("${jwt.defaultExpiration}")
    private long DEFAULT_TTL;

    /*──────────────────────────────────────────────────*/
    /*  POST /auth/login                                */
    /*──────────────────────────────────────────────────*/
    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponseModel>> login(
            @RequestBody LoginRegisterRequest req,
            ServerWebExchange ex) {

        log.info("LOGIN attempt for {}", req.username());

        return authenticate(req.username(), req.password())
                .flatMap(p -> buildResponse(p, req.ttlSeconds(), ex))
                .doOnSuccess(r -> log.info("LOGIN success for {}", req.username()))
                .doOnError(e -> log.error("LOGIN failed for {}", req.username(), e));
    }

    /*──────────────────────────────────────────────────*/
    /*  POST /auth/register                             */
    /*──────────────────────────────────────────────────*/
    @PostMapping("/register")
    public Mono<ResponseEntity<LoginResponseModel>> register(
            @RequestBody LoginRegisterRequest req,
            ServerWebExchange ex) {

        log.info("REGISTER attempt for {}", req.username());

        return userService.register(req)
                .flatMap(u -> authenticate(u.getUsername(), req.password()))
                .flatMap(p -> buildResponse(p, req.ttlSeconds(), ex))
                .doOnSuccess(r -> log.info("REGISTER success for {}", req.username()))
                .doOnError(e -> log.error("REGISTER failed for {}", req.username(), e));
    }

    /*──────────────────────────────────────────────────*/
    /*  Shared helpers                                  */
    /*──────────────────────────────────────────────────*/

    private Mono<CustomUserPrincipal> authenticate(String user, String pw) {
        return authManager.authenticate(
                        new UsernamePasswordAuthenticationToken(user, pw))
                .switchIfEmpty(Mono.error(new BadCredentialsException("Bad credentials")))
                .map(auth -> (CustomUserPrincipal) auth.getPrincipal());
    }

    private Mono<ResponseEntity<LoginResponseModel>> buildResponse(
            CustomUserPrincipal principal,
            Long ttlSeconds,
            ServerWebExchange ex) {

        ResponseCookie cookie = issueToken(principal, ttlSeconds);

        LoginResponseModel model = new LoginResponseModel(
                principal.getId(),
                principal.getUsername(),
                principal.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList());

        /* HAL link built synchronously */
        String href = ex.getRequest()
                .getURI()
                .resolve("/users/" + principal.getId())
                .toString();
        model.add(Link.of(href, "user"));

        return Mono.just(ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(model));
    }

    private ResponseCookie issueToken(CustomUserPrincipal principal, Long ttlSeconds) {
        long ttl = (ttlSeconds == null || ttlSeconds <= 0) ? DEFAULT_TTL : ttlSeconds;
        String token = jwt.generate(principal, Duration.ofSeconds(ttl));

        return ResponseCookie.from("ACCESS_TOKEN", token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofSeconds(ttl))
                .build();
    }

    /*──────────────────────────────────────────────────*/
    /*  DTOs                                            */
    /*──────────────────────────────────────────────────*/

    public record LoginRegisterRequest(String username,
                                       String password,
                                       Long ttlSeconds) {
    }
}
