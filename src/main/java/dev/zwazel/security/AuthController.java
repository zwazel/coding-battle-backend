package dev.zwazel.security;

import dev.zwazel.domain.User;
import dev.zwazel.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRegisterRequest req) {
        // 1. delegate to AuthenticationManager (uses our UserDetailsService + PW encoder)
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username(), req.password()));

        // 2. decide TTL
        Duration ttl = req.rememberMe()
                ? Duration.ofDays(30)
                : (req.expiresInSeconds() != null
                ? Duration.ofSeconds(req.expiresInSeconds())
                : Duration.ofMinutes(15));

        // 3. create JWT
        String token = jwt.generate((UserDetails) auth.getPrincipal(), ttl);

        // http-only cookie
        ResponseCookie cookie = ResponseCookie.from("ACCESS_TOKEN", token)
                .httpOnly(true).secure(true).path("/").maxAge(ttl).sameSite("Strict").build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).build();
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@RequestBody LoginRegisterRequest req) {
        // 1. delegate to UserService to create a new user
        User user = userService.register(req);

        // 2. authenticate the new user
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), req.password()));

        // 3. decide TTL
        Duration ttl = req.rememberMe()
                ? Duration.ofDays(30)
                : (req.expiresInSeconds() != null
                ? Duration.ofSeconds(req.expiresInSeconds())
                : Duration.ofMinutes(15));

        // 4. create JWT
        String token = jwt.generate((UserDetails) auth.getPrincipal(), ttl);

        // http-only cookie
        ResponseCookie cookie = ResponseCookie.from("ACCESS_TOKEN", token)
                .httpOnly(true).secure(true).path("/").maxAge(ttl).sameSite("Strict").build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new LoginResponse("Bearer", token, ttl.getSeconds()));
    }

    public record LoginRegisterRequest(String username, String password,
                                       boolean rememberMe,
                                       Long expiresInSeconds) {
    }

    public record LoginResponse(String tokenType, String token, long expiresIn) {
    }
}
