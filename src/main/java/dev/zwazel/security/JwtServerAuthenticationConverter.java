package dev.zwazel.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
class JwtServerAuthenticationConverter implements ServerAuthenticationConverter {

    private static final String BEARER = "Bearer ";

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        String token = resolve(exchange);
        log.info("Converting token: {}", token);

        return token == null
                ? Mono.empty()
                : Mono.just(new UsernamePasswordAuthenticationToken(token, token));
    }

    private String resolve(ServerWebExchange ex) {
        String h = ex.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (h != null && h.startsWith(BEARER)) return h.substring(BEARER.length());
        HttpCookie c = ex.getRequest().getCookies().getFirst("ACCESS_TOKEN");
        return c != null ? c.getValue() : null;
    }
}
