package dev.zwazel.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest req, @NonNull HttpServletResponse res, @NonNull FilterChain chain) throws ServletException, IOException {
        String token = resolve(req);            // header or cookie
        if (token != null) {
            try {
                var jws = jwt.parse(token);
                String username = jws.getPayload().getSubject();

                List<SimpleGrantedAuthority> auths =
                        ((List<String>) jws.getPayload().get("roles"))
                                .stream().map(SimpleGrantedAuthority::new).toList();

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(username, null, auths);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException e) {
                // invalid / expired → leave context empty
            }
        }
        chain.doFilter(req, res);
    }

    private String resolve(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) return h.substring(7);
        Cookie c = WebUtils.getCookie(req, "ACCESS_TOKEN");
        return c != null ? c.getValue() : null;
    }
}
