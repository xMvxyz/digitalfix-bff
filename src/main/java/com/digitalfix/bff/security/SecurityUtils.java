package com.digitalfix.bff.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static String extractEmail(Authentication auth) {
        if (auth == null) return "anon@digitalfix.cl";
        Object principal = auth.getName();
        if (principal != null && principal.toString().contains("@")) return principal.toString();
        if (auth instanceof JwtAuthenticationToken jwt) {
            String email = jwt.getToken().getClaimAsString("email");
            if (email == null) email = jwt.getToken().getClaimAsString("preferred_username");
            if (email == null) email = jwt.getToken().getClaimAsString("upn");
            if (email != null) return email;
        }
        return auth.getName() != null ? auth.getName() : "anon@digitalfix.cl";
    }

    public static String extractRole(Authentication auth) {
        if (auth == null) return "Cliente";
        for (GrantedAuthority ga : auth.getAuthorities()) {
            String r = ga.getAuthority().replace("ROLE_", "");
            if (r.equalsIgnoreCase("ADMIN") || r.equalsIgnoreCase("SUPERVISOR") || r.equalsIgnoreCase("CLIENTE")) {
                // Capitaliza primera letra
                return r.substring(0, 1).toUpperCase() + r.substring(1).toLowerCase();
            }
        }
        // fallback: busca claim roles original
        if (auth instanceof JwtAuthenticationToken jwt) {
            var roles = jwt.getToken().getClaimAsStringList("roles");
            if (roles != null && !roles.isEmpty()) {
                String r = roles.get(0);
                return r.substring(0, 1).toUpperCase() + r.substring(1).toLowerCase();
            }
        }
        return "Cliente";
    }

    public static boolean isPrivileged(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equalsIgnoreCase("ROLE_ADMIN") ||
                a.getAuthority().equalsIgnoreCase("ROLE_SUPERVISOR") ||
                a.getAuthority().equalsIgnoreCase("ROLE_Admin") ||
                a.getAuthority().equalsIgnoreCase("ROLE_Supervisor")
        );
    }
}
