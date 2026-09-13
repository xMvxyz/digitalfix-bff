package com.digitalfix.bff.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public class JwtAudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final List<String> allowedAudiences;

    public JwtAudienceValidator(List<String> allowedAudiences) {
        this.allowedAudiences = allowedAudiences;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        List<String> audiences = jwt.getAudience();
        if (audiences == null || audiences.isEmpty()) {
            // Entra ID a veces usa claim "aud" como string, Spring lo mapea a audience
            // Si no hay audience pero hay claim "aud" string, se considera valido si coincide
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Audience vacia", null));
        }
        boolean match = audiences.stream().anyMatch(allowedAudiences::contains);
        if (match) return OAuth2TokenValidatorResult.success();
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Audience no autorizada: " + audiences, null));
    }
}
