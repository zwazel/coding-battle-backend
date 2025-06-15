package dev.zwazel.security;

import dev.zwazel.domain.User;
import dev.zwazel.service.UserService;
import lombok.RequiredArgsConstructor;
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
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        return issueToken((CustomUserPrincipal) auth.getPrincipal(), req.ttlSeconds());
    }

    /* ----------- POST /auth/register -------------- */
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@RequestBody LoginRegisterRequest req) {
        User newUser = userService.register(req);   // throws if a username exists
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(newUser.getUsername(), req.password()));
        return issueToken((CustomUserPrincipal) auth.getPrincipal(), req.ttlSeconds());
    }

    /* -------- helper: mint JWT + cookie ----------- */
    private ResponseEntity<LoginResponse> issueToken(CustomUserPrincipal principal, Long ttlSeconds) {
        long ttl = (ttlSeconds == null || ttlSeconds <= 0) ? DEFAULT_TTL : ttlSeconds;
        Duration duration = Duration.ofSeconds(ttl);

        String token = jwt.generate(principal, duration);

        ResponseCookie cookie = ResponseCookie.from("ACCESS_TOKEN", token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(duration)
                .build();

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
