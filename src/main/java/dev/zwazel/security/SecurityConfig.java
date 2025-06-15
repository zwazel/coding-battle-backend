package dev.zwazel.security;

import dev.zwazel.service.UserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
class SecurityConfig {
    private final JwtAuthFilter jwtFilter;

    private final Environment env;

    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String corsAllowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String corsAllowedMethods;

    @Value("${cors.allowed-headers:*}")
    private String corsAllowedHeaders;

    @Value("${cors.allow-credentials:true}")
    private boolean corsAllowCredentials;

    @Bean
    SecurityFilterChain chain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                /* Frame header disabled only for H2 console */
                .headers(h -> h.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))

                /* Path-based rules */
                .authorizeHttpRequests(auth -> auth
                        /* open to everyone */
                        .requestMatchers(
                                /* Public */
                                AntPathRequestMatcher.antMatcher("/api/**/public/**"),
                                /* Auth */
                                AntPathRequestMatcher.antMatcher("/api/auth/**"),
                                /* Lobbies */
                                AntPathRequestMatcher.antMatcher("/api/lobbies/**")
                        )
                        .permitAll()

                        /* ADMIN role */
                        .requestMatchers(
                                AntPathRequestMatcher.antMatcher("/api/**/admin/**")
                        ).hasRole("ADMIN")

                        /* USER role */
                        .requestMatchers(
                                AntPathRequestMatcher.antMatcher("/api/bots/**"),
                                AntPathRequestMatcher.antMatcher("/api/users/**")
                        ).hasRole("USER")
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

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(corsAllowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList(corsAllowedMethods.split(",")));
        configuration.setAllowedHeaders(Arrays.asList(corsAllowedHeaders.split(",")));
        configuration.setAllowCredentials(corsAllowCredentials);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
