package dev.zwazel.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableMethodSecurity
class SecurityConfig {
    @Bean
    SecurityFilterChain filterChainGeneral(HttpSecurity http) throws Exception {
        http
                /* CSRF default is fine for APIs that use JWT or same-site cookies.
                   For pure JSON REST with token auth you’d disable it. */
                .csrf(AbstractHttpConfigurer::disable)

                /* Frame header disabled only for H2 console */
                .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))



                /* Use HTTP Basic for quick tests, or formLogin(), or JWT. */
                .httpBasic(Customizer.withDefaults());

        if (System.getProperty("spring.profiles.active", "").contains("dev")) {
            // allow H2 console
            http
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(PathRequest.toH2Console()).permitAll()
                    )
            ;
        }

        return http.build();
    }
}
