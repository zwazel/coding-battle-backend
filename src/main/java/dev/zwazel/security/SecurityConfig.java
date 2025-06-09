package dev.zwazel.security;

import dev.zwazel.service.UserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
class SecurityConfig {
    private final JwtAuthFilter jwtFilter;

    private final Environment env;

    @Bean
    SecurityFilterChain filterChainGeneral(HttpSecurity http) throws Exception {
        http
                /* CSRF default is fine for APIs that use JWT or same-site cookies.
                   For pure JSON REST with token auth you’d disable it. */
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                /* Frame header disabled only for H2 console */
                .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))

                /* Path-based rules */
                .authorizeHttpRequests(auth -> auth
                        /* open to everyone */
                        .requestMatchers(
                                AntPathRequestMatcher.antMatcher("/api/**/public/**"),
                                AntPathRequestMatcher.antMatcher("/api/auth/**")
                        )
                        .permitAll()

                        /* ADMIN role */
                        .requestMatchers(
                                AntPathRequestMatcher.antMatcher("/api/**/admin/**")
                        ).hasRole("ADMIN")

                        /* USER role */
                        .requestMatchers(
                                AntPathRequestMatcher.antMatcher("/api/**/user/**")
                        ).hasRole("USER")

                        /* Lobbies */
                        .requestMatchers(
                                AntPathRequestMatcher.antMatcher("/api/lobbies/**")
                        ).permitAll()
                )

                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        if (env.matchesProfiles("dev")) {
            // allow H2 console
            http
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(PathRequest.toH2Console()).permitAll()
                    )
            ;
        }

        /* Any other request requires authentication */
        http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    // expose AuthenticationManager that AuthController uses
    @Bean
    AuthenticationManager authManager(UserDetailsService uds, PasswordEncoder enc) {
        DaoAuthenticationProvider dao = new DaoAuthenticationProvider();
        dao.setUserDetailsService(uds);
        dao.setPasswordEncoder(enc);
        return new ProviderManager(dao);
    }

    @Bean
    PasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }
}
