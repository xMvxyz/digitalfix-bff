package com.digitalfix.bff.config;

import com.digitalfix.bff.security.JwtAudienceValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;

import java.util.List;

@Configuration
@ConditionalOnProperty(name = "app.security-disabled", havingValue = "false", matchIfMissing = true)
public class JwtValidatorConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.audiences:api://digitalfix-bff}")
    private String audiences;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
    private String issuerUri;

    @Bean
    public OAuth2TokenValidator<Jwt> jwtValidator() {
        List<String> audList = List.of(audiences.split(",")).stream().map(String::trim).toList();
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtAudienceValidator(audList);
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        return new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);
    }

    @Bean
    public JwtDecoder jwtDecoder(OAuth2TokenValidator<Jwt> jwtValidator) {
        // Usa issuer-uri para discovery, si JWK_SET_URI viene vacio
        NimbusJwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuerUri);
        decoder.setJwtValidator(jwtValidator);
        return decoder;
    }
}
