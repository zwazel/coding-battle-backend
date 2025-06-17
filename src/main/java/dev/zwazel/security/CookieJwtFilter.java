package dev.zwazel.security;

import io.micrometer.common.lang.NonNullApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@NonNullApi
@Slf4j
class CookieJwtFilter implements WebFilter {
    private final JwtUtil jwtUtil;

    @Value("${jwt.cookie.name}")
    private String jwtCookieName;

    @Override
    public Mono<Void> filter(ServerWebExchange ex, WebFilterChain chain) {
        log.debug("Starting filter for request: {}", ex.getRequest().getURI());

        var cookie = ex.getRequest().getCookies().getFirst(jwtCookieName);
        if (cookie == null) {
            return chain.filter(ex);
        }

        return jwtUtil.validate(cookie.getValue())
                .doOnSuccess(auth -> log.debug("JWT successfully validated"))
                .doOnError(e -> log.warn("Error during JWT validation: {}", e.getMessage()))
                .flatMap(auth -> {
                    log.debug("Setting authentication in security context");
                    return chain.filter(ex)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
                })
                .onErrorResume(e -> {
                    log.error("JWT validation failed: {}", e.getMessage());
                    return chain.filter(ex);
                });
    }

}
