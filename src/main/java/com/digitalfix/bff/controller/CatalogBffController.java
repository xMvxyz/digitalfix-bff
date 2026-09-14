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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class CatalogBffController {

    private final RestTemplate restTemplate;

    // Asegurate de que este puerto coincida con tu microservicio de catalogo (ej: 8081)
    @Value("${app.catalog-url:http://localhost:8081}")
    private String baseUrl;

    // ==========================================
    // SERVICIOS
    // ==========================================
    @GetMapping("/api/catalog/servicios")
    public ResponseEntity<String> listServicios(@RequestHeader HttpHeaders headers, 
                                                @RequestParam(required = false) String soloActivos, 
                                                Authentication auth) {
        String url = baseUrl + "/api/catalog/servicios" + (soloActivos != null ? "?soloActivos=" + soloActivos : "");
        return exchange(url, HttpMethod.GET, headers, null, auth);
    }

    @GetMapping("/api/catalog/servicios/{id}")
    public ResponseEntity<String> getServicio(@PathVariable String id, @RequestHeader HttpHeaders headers, Authentication auth) {
        return exchange(baseUrl + "/api/catalog/servicios/" + id, HttpMethod.GET, headers, null, auth);
    }

    @PostMapping("/api/catalog/servicios")
    public ResponseEntity<String> createServicio(@RequestHeader HttpHeaders headers, @RequestBody(required = false) String body, Authentication auth) {
        return exchange(baseUrl + "/api/catalog/servicios", HttpMethod.POST, headers, body, auth);
    }

    @PutMapping("/api/catalog/servicios/{id}")
    public ResponseEntity<String> updateServicio(@PathVariable String id, @RequestHeader HttpHeaders headers, @RequestBody(required = false) String body, Authentication auth) {
        return exchange(baseUrl + "/api/catalog/servicios/" + id, HttpMethod.PUT, headers, body, auth);
    }

    @DeleteMapping("/api/catalog/servicios/{id}")
    public ResponseEntity<String> deleteServicio(@PathVariable String id, @RequestHeader HttpHeaders headers, Authentication auth) {
        return exchange(baseUrl + "/api/catalog/servicios/" + id, HttpMethod.DELETE, headers, null, auth);
    }

    // ==========================================
    // REPUESTOS
    // ==========================================
    @GetMapping("/api/catalog/repuestos")
    public ResponseEntity<String> listRepuestos(@RequestHeader HttpHeaders headers, Authentication auth) {
        return exchange(baseUrl + "/api/catalog/repuestos", HttpMethod.GET, headers, null, auth);
    }

    @GetMapping("/api/catalog/repuestos/stock-bajo")
    public ResponseEntity<String> stockBajo(@RequestHeader HttpHeaders headers, Authentication auth) {
        return exchange(baseUrl + "/api/catalog/repuestos/stock-bajo", HttpMethod.GET, headers, null, auth);
    }

    @GetMapping("/api/catalog/repuestos/{id}")
    public ResponseEntity<String> getRepuesto(@PathVariable String id, @RequestHeader HttpHeaders headers, Authentication auth) {
        return exchange(baseUrl + "/api/catalog/repuestos/" + id, HttpMethod.GET, headers, null, auth);
    }

    @PostMapping("/api/catalog/repuestos")
    public ResponseEntity<String> createRepuesto(@RequestHeader HttpHeaders headers, @RequestBody(required = false) String body, Authentication auth) {
        return exchange(baseUrl + "/api/catalog/repuestos", HttpMethod.POST, headers, body, auth);
    }

    @PutMapping("/api/catalog/repuestos/{id}")
    public ResponseEntity<String> updateRepuesto(@PathVariable String id, @RequestHeader HttpHeaders headers, @RequestBody(required = false) String body, Authentication auth) {
        return exchange(baseUrl + "/api/catalog/repuestos/" + id, HttpMethod.PUT, headers, body, auth);
    }

    @DeleteMapping("/api/catalog/repuestos/{id}")
    public ResponseEntity<String> deleteRepuesto(@PathVariable String id, @RequestHeader HttpHeaders headers, Authentication auth) {
        return exchange(baseUrl + "/api/catalog/repuestos/" + id, HttpMethod.DELETE, headers, null, auth);
    }

    @PostMapping("/api/catalog/repuestos/{id}/descontar")
    public ResponseEntity<String> descontar(@PathVariable String id, @RequestHeader HttpHeaders headers, @RequestBody(required = false) String body, Authentication auth) {
        return exchange(baseUrl + "/api/catalog/repuestos/" + id + "/descontar", HttpMethod.POST, headers, body, auth);
    }

    @PostMapping("/api/catalog/repuestos/{id}/reponer")
    public ResponseEntity<String> reponer(@PathVariable String id, @RequestHeader HttpHeaders headers, @RequestBody(required = false) String body, Authentication auth) {
        return exchange(baseUrl + "/api/catalog/repuestos/" + id + "/reponer", HttpMethod.POST, headers, body, auth);
    }

    @PostMapping("/api/catalog/repuestos/{id}/consumir")
    public ResponseEntity<String> consumir(@PathVariable String id, @RequestHeader HttpHeaders headers, Authentication auth) {
        return exchange(baseUrl + "/api/catalog/repuestos/" + id + "/consumir", HttpMethod.POST, headers, null, auth);
    }

    // ==========================================
    // COMUNICACIÓN HTTP
    // ==========================================
    private ResponseEntity<String> exchange(String url, HttpMethod method, HttpHeaders incomingHeaders, String body, Authentication auth) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        String authorization = incomingHeaders.getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authorization);
        }

        headers.set("X-User-Email", resolveEmail(auth));
        headers.set("X-User-Role", resolveRole(auth));

        try {
            return restTemplate.exchange(url, method, new HttpEntity<>(body, headers), String.class);
        } catch (HttpStatusCodeException ex) {
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