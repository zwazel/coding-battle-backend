package dev.zwazel.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
@EnableReactiveMethodSecurity
class SecurityConfig {
    private final Environment env;

    @Value("${roles.admin}")
    private String adminRoleName;

    @Value("${roles.user}")
    private String userRoleName;

    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String corsAllowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String corsAllowedMethods;

    @Value("${cors.allowed-headers:*}")
    private String corsAllowedHeaders;

    @Value("${cors.allow-credentials:true}")
    private boolean corsAllowCredentials;

    @Bean
    SecurityWebFilterChain springSecurityFilterChain(
            ServerHttpSecurity http,
            JwtAuthenticationManager authMgr,
            JwtServerAuthenticationConverter conv,
            CorsConfigurationSource corsConfigurationSource
    ) {
        AuthenticationWebFilter jwtFilter = new AuthenticationWebFilter(authMgr);
        jwtFilter.setServerAuthenticationConverter(conv);

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                //.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeExchange(ex -> ex
                        /* Open to everyone */
                        .pathMatchers(
                                /* Public */
                                "/public/**",
                                /* Auth */
                                "/auth/**",
                                /* Lobbies */
                                "/lobbies/**"
                        )
                        .permitAll()

                        /* ADMIN role */
                        .pathMatchers("/admin/**").hasRole(adminRoleName)

                        /* USER role */
                        .pathMatchers(
                                "/bots/**",
                                "/users/**"
                        ).hasRole(userRoleName)
                )
                .addFilterAt(jwtFilter, SecurityWebFiltersOrder.AUTHENTICATION)
        ;
        /*.exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new JwtAuthenticationEntryPoint(env))
                        .accessDeniedHandler(new JwtAccessDeniedHandler(env)))*/

        if (env.matchesProfiles("dev")) {
            http.authorizeExchange(ex -> ex
                    .pathMatchers("/h2-console/**").permitAll()
            );
        }

        /* Any other request requires authentication */
        http
                .authorizeExchange(ex -> ex
                        .anyExchange().authenticated()
                );

        return http.build();
    }

    /*// expose AuthenticationManager that AuthController uses
    @Bean
    ReactiveAuthenticationManager authManager(UserDetailsService uds,
                                              PasswordEncoder encoder) {

        UserDetailsRepositoryReactiveAuthenticationManager auth =
                new UserDetailsRepositoryReactiveAuthenticationManager(uds);

        auth.setPasswordEncoder(encoder);
        return auth;
    }*/

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
