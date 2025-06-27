package dev.zwazel.security;

import dev.zwazel.domain.User;
import dev.zwazel.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final UserService userService;
    @Value("${jwt.defaultExpiration}")
    private long DEFAULT_TTL;   // 15 min

    /* ------------ POST /auth/login ---------------- */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRegisterRequest req) {
        log.info("Login attempt for user: {}", req.username());
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        log.info("User {} authenticated successfully", req.username());
        return issueToken((CustomUserPrincipal) auth.getPrincipal(), req.ttlSeconds());
    }

    /* ----------- POST /auth/register -------------- */
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@RequestBody LoginRegisterRequest req) {
        log.info("Registration attempt for user: {}", req.username());
        User newUser = userService.register(req);   // throws if a username exists
        log.info("User {} registered successfully", newUser.getUsername());
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(newUser.getUsername(), req.password()));
        log.info("User {} authenticated successfully after registration", newUser.getUsername());
        return issueToken((CustomUserPrincipal) auth.getPrincipal(), req.ttlSeconds());
    }

    /* -------- helper: mint JWT + cookie ----------- */
    private ResponseEntity<LoginResponse> issueToken(CustomUserPrincipal principal, Long ttlSeconds) {
        long ttl = (ttlSeconds == null || ttlSeconds <= 0) ? DEFAULT_TTL : ttlSeconds;
        Duration duration = Duration.ofSeconds(ttl);

        log.info("Issuing token for user {} with a ttl of {} seconds", principal.getUsername(), ttl);

        String token = jwt.generate(principal, duration);

        ResponseCookie cookie = ResponseCookie.from("ACCESS_TOKEN", token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(duration)
                .build();

        log.info("Cookie created for user {}", principal.getUsername());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new LoginResponse(principal.getId(), "Bearer", token, ttl));
    }

    /* ------------- DTOs --------------------------- */
    public record LoginRegisterRequest(String username, String password, Long ttlSeconds) {
    }

    public record LoginResponse(UUID id, String tokenType, String token, long expiresInSeconds) {
    }
}
