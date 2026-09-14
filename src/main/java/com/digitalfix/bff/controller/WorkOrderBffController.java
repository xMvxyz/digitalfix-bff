package com.digitalfix.bff.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/workorders")
@RequiredArgsConstructor
public class WorkOrderBffController {

    private final RestTemplate restTemplate;

    // Asegurate de que este puerto coincida con tu microservicio de ordenes (ej: 8082)
    @Value("${app.workorders-url:http://localhost:8082}")
    private String baseUrl;

    @GetMapping
    public ResponseEntity<String> list(@RequestHeader HttpHeaders headers, Authentication auth) {
        return exchange(baseUrl + "/api/workorders", HttpMethod.GET, headers, null, auth);
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> get(@PathVariable String id, @RequestHeader HttpHeaders headers, Authentication auth) {
        return exchange(baseUrl + "/api/workorders/" + id, HttpMethod.GET, headers, null, auth);
    }

    @PostMapping
    public ResponseEntity<String> create(@RequestHeader HttpHeaders headers, @RequestBody(required = false) String body, Authentication auth) {
        return exchange(baseUrl + "/api/workorders", HttpMethod.POST, headers, body, auth);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<String> changeStatus(@PathVariable String id, @RequestHeader HttpHeaders headers, @RequestBody(required = false) String body, Authentication auth) {
        return exchange(baseUrl + "/api/workorders/" + id + "/status", HttpMethod.PATCH, headers, body, auth);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable String id, @RequestHeader HttpHeaders headers, Authentication auth) {
        return exchange(baseUrl + "/api/workorders/" + id, HttpMethod.DELETE, headers, null, auth);
    }

    private ResponseEntity<String> exchange(String url, HttpMethod method, HttpHeaders incomingHeaders, String body, Authentication auth) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        // Propagar el token Bearer si viene presente
        String authorization = incomingHeaders.getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authorization);
        }

        // Propagar identidad y rol extraidos de forma segura desde el JWT
        headers.set("X-User-Email", resolveEmail(auth));
        headers.set("X-User-Role", resolveRole(auth));

        try {
            return restTemplate.exchange(url, method, new HttpEntity<>(body, headers), String.class);
        } catch (HttpStatusCodeException ex) {
            // Si el microservicio devuelve 400, 404, 409, etc., reenviamos el JSON de error exacto
            return ResponseEntity
                    .status(ex.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ex.getResponseBodyAsString());
        }
    }

    private String resolveEmail(Authentication auth) {
        if (auth == null) return "anonymous";
        if (auth.getPrincipal() instanceof Jwt jwt) {
            if (jwt.hasClaim("preferred_username")) return jwt.getClaimAsString("preferred_username");
            if (jwt.hasClaim("email")) return jwt.getClaimAsString("email");
            if (jwt.hasClaim("upn")) return jwt.getClaimAsString("upn");
            return jwt.getSubject();
        }
        return auth.getName();
    }

    private String resolveRole(Authentication auth) {
        if (auth == null) return "ANONYMOUS";
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .findFirst()
                .orElse("Cliente");
    }
}