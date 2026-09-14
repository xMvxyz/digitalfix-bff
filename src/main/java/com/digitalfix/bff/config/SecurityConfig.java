package com.digitalfix.bff.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.digitalfix.bff.security.JwtRoleConverter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.security-disabled:false}")
    private boolean securityDisabled;

    @Value("${app.cors-allowed-origins:http://localhost:4200,http://localhost:8100}")
    private String corsOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        if (securityDisabled) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
            return http.build();
        }

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Catalogo: el Cliente puede consultar; las mutaciones quedan para Admin/Supervisor.
                .requestMatchers(HttpMethod.GET, "/api/catalog/**").hasAnyRole("ADMIN", "SUPERVISOR", "CLIENTE", "Admin", "Supervisor", "Cliente")
                .requestMatchers("/api/catalog/**").hasAnyRole("ADMIN", "SUPERVISOR", "Admin", "Supervisor")
                // El caso permite al Cliente cambiar el estado de sus ordenes; el microservicio valida la transicion.
                .requestMatchers(HttpMethod.PATCH, "/api/workorders/*/status").hasAnyRole("ADMIN", "SUPERVISOR", "CLIENTE", "Admin", "Supervisor", "Cliente")
                .requestMatchers(HttpMethod.DELETE, "/api/workorders/**").hasAnyRole("ADMIN", "SUPERVISOR", "Admin", "Supervisor")
                .requestMatchers("/api/workorders/**").authenticated()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(401);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Token requerido o invalido\"}");
                })
                .accessDeniedHandler((req, res, e) -> {
                    res.setStatus(403);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Permiso insuficiente para el rol\"}");
                })
            );
        return http.build();
    }

    @Bean
    public org.springframework.core.convert.converter.Converter<org.springframework.security.oauth2.jwt.Jwt, org.springframework.security.authentication.AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return new JwtRoleConverter();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(corsOrigins.split(",")).map(String::trim).toList();
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
