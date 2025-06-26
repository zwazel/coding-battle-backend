package dev.zwazel.security;

import dev.zwazel.service.UserDetailsService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req, @NonNull HttpServletResponse res, @NonNull FilterChain chain) throws ServletException, IOException {
        log.debug("Processing request: {}", req.getRequestURI());

        String token = resolve(req);
        if (token != null) {
            log.debug("Found JWT token in request");
            try {
                var jws = jwt.parse(token);
                String username = jws.getPayload().getSubject();
                log.debug("Successfully parsed JWT token for user: {}", username);

                // Load full user details to ensure the user exists
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Build auth with that principal
                var auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(auth);
                log.info("Successfully authenticated user: {}", username);
            } catch (JwtException | UsernameNotFoundException e) {
                log.warn("Failed to authenticate user from JWT token", e);
                // invalid / expired → leave context empty
            }
        }

        // Continue with the filter chain
        log.debug("Continuing filter chain for request: {}", req.getRequestURI());
        chain.doFilter(req, res);
    }

    private String resolve(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            String token = h.substring(7);
            log.debug("Resolved JWT token from Authorization header");
            return token;
        }
        Cookie c = WebUtils.getCookie(req, "ACCESS_TOKEN");
        if (c != null) {
            String token = c.getValue();
            log.debug("Resolved JWT token from ACCESS_TOKEN cookie");
            return token;
        }
        log.debug("No JWT token found in request");
        return null;
    }
}
