package com.digitalfix.bff.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JwtRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = Stream.concat(
                defaultConverter.convert(jwt).stream(),
                extractRoles(jwt).stream()
        ).collect(Collectors.toSet());

        String principal = jwt.getClaimAsString("preferred_username");
        if (principal == null) principal = jwt.getClaimAsString("upn");
        if (principal == null) principal = jwt.getClaimAsString("email");
        if (principal == null) principal = jwt.getSubject();

        return new JwtAuthenticationToken(jwt, authorities, principal);
    }

    private Collection<GrantedAuthority> extractRoles(Jwt jwt) {
        // Entra ID usa claim "roles" para App Roles. También soportamos "groups" y "scp"
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null) roles = jwt.getClaimAsStringList("groups");
        if (roles == null) {
            String scp = jwt.getClaimAsString("scp");
            if (scp != null) roles = List.of(scp.split(" "));
            else roles = Collections.emptyList();
        }
        return roles.stream()
                .map(r -> r.toUpperCase().startsWith("ROLE_") ? r.toUpperCase() : "ROLE_" + r.toUpperCase())
                // Normaliza nombres tipo "Admin" -> ROLE_ADMIN, "admin" -> ROLE_ADMIN
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
