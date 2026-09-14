package com.digitalfix.bff.config;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.audiences}")
    private String expectedAudience;

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
                // Catalogo: lectura para todos los roles; la gestión es exclusiva del Supervisor.
                .requestMatchers(HttpMethod.GET, "/api/catalog/**").hasAnyRole("ADMIN", "SUPERVISOR", "CLIENTE")
                .requestMatchers("/api/catalog/**").hasRole("SUPERVISOR")
                // El caso permite al Cliente cambiar el estado de sus ordenes; el microservicio valida la transicion.
                .requestMatchers(HttpMethod.PATCH, "/api/workorders/*/status").hasAnyRole("ADMIN", "SUPERVISOR", "CLIENTE")
                .requestMatchers(HttpMethod.DELETE, "/api/workorders/**").hasAnyRole("ADMIN", "SUPERVISOR")
                .requestMatchers("/api/workorders/**").authenticated()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter()))
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
    public Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
        return jwt -> {
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles == null || roles.isEmpty()) {
                return Collections.emptyList();
            }
            return roles.stream()
                .map(role -> "ROLE_" + role.replaceFirst("(?i)^ROLE_", "").toUpperCase())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        };
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
        return converter;
    }

    @Bean
    @ConditionalOnProperty(name = "app.security-disabled", havingValue = "false", matchIfMissing = true)
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuerUri);
        OAuth2TokenValidator<Jwt> issuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        // Entra ID v1 emite aud "api://GUID" y v2 emite el GUID pelado: se normaliza antes de comparar.
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience().stream()
            .map(SecurityConfig::normalizeAudience)
            .anyMatch(a -> a.equalsIgnoreCase(normalizeAudience(expectedAudience)))
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Audience no valida", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuer, audience));
        return decoder;
    }

    private static String normalizeAudience(String aud) {
        if (aud == null) return "";
        String s = aud.trim();
        if (s.regionMatches(true, 0, "api://", 0, 6)) s = s.substring(6);
        int slash = s.indexOf('/');
        if (slash > 0) s = s.substring(0, slash);
        return s;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(corsOrigins.split(",")).map(String::trim).toList();
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // Explicito para igualar API Gateway + allowCredentials=true (* falla en algunos navegadores/Spring)
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-User-Email", "X-User-Role"));
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
