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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/workorders")
@RequiredArgsConstructor
public class WorkOrderBffController {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Asegurate de que este puerto coincida con tu microservicio de ordenes (ej: 8082)
    @Value("${app.workorders-url:http://localhost:8082}")
    private String baseUrl;

    @Value("${app.catalog-url:http://localhost:8081}")
    private String catalogBaseUrl;

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
        // Pre-check stock antes de ASIGNAR para no dejar orden inconsistente
        Long preRepuestoId = null;
        if (isAsignada(body)) {
            preRepuestoId = fetchRepuestoId(id, headers, auth);
            if (preRepuestoId != null && !hasStock(preRepuestoId, headers, auth)) {
                return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":409,\"error\":\"Conflict\",\"message\":\"Stock insuficiente para repuesto " + preRepuestoId + ", no se asigna la orden\"}");
            }
        }
        ResponseEntity<String> response = exchange(baseUrl + "/api/workorders/" + id + "/status", HttpMethod.PATCH, headers, body, auth);
        // Regla clave: el stock del repuesto disminuye al asignar la orden
        if (response.getStatusCode().is2xxSuccessful() && isAsignada(body)) {
            Long repuestoId = extractRepuestoId(response.getBody());
            if (repuestoId == null) repuestoId = preRepuestoId;
            if (repuestoId != null) {
                try {
                    restTemplate.exchange(catalogBaseUrl + "/api/catalog/repuestos/" + repuestoId + "/consumir",
                            HttpMethod.POST, new HttpEntity<>(null, forwardHeaders(headers, auth)), String.class);
                } catch (HttpStatusCodeException ex) {
                    return ResponseEntity
                            .status(org.springframework.http.HttpStatus.CONFLICT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(ex.getResponseBodyAsString());
                } catch (RestClientException ex) {
                    return ResponseEntity
                            .status(org.springframework.http.HttpStatus.BAD_GATEWAY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"status\":502,\"error\":\"Bad Gateway\",\"message\":\"Orden asignada pero no se pudo descontar stock: " + ex.getMessage() + "\"}");
                }
            }
        }
        return response;
    }

    private Long fetchRepuestoId(String id, HttpHeaders headers, Authentication auth) {
        try {
            ResponseEntity<String> r = restTemplate.exchange(baseUrl + "/api/workorders/" + id,
                    HttpMethod.GET, new HttpEntity<>(null, forwardHeaders(headers, auth)), String.class);
            return extractRepuestoId(r.getBody());
        } catch (Exception e) {
            return null;
        }
    }

    private boolean hasStock(Long repuestoId, HttpHeaders headers, Authentication auth) {
        try {
            ResponseEntity<String> r = restTemplate.exchange(catalogBaseUrl + "/api/catalog/repuestos/" + repuestoId,
                    HttpMethod.GET, new HttpEntity<>(null, forwardHeaders(headers, auth)), String.class);
            JsonNode n = objectMapper.readTree(r.getBody());
            return n.path("stock").asInt(1) > 0;
        } catch (Exception e) {
            return true;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable String id, @RequestHeader HttpHeaders headers, Authentication auth) {
        return exchange(baseUrl + "/api/workorders/" + id, HttpMethod.DELETE, headers, null, auth);
    }

    private ResponseEntity<String> exchange(String url, HttpMethod method, HttpHeaders incomingHeaders, String body, Authentication auth) {
        try {
            return restTemplate.exchange(url, method, new HttpEntity<>(body, forwardHeaders(incomingHeaders, auth)), String.class);
        } catch (HttpStatusCodeException ex) {
            // Si el microservicio devuelve 400, 404, 409, etc., reenviamos el JSON de error exacto
            return ResponseEntity
                    .status(ex.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ex.getResponseBodyAsString());
        }
    }

    private HttpHeaders forwardHeaders(HttpHeaders incomingHeaders, Authentication auth) {
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
        return headers;
    }

    private boolean isAsignada(String body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            return node.path("estado").asText("").equalsIgnoreCase("ASIGNADA");
        } catch (Exception e) {
            return false;
        }
    }

    private Long extractRepuestoId(String responseBody) {
        try {
            JsonNode node = objectMapper.readTree(responseBody);
            JsonNode id = node.path("repuestoId");
            return (id.isNumber()) ? id.asLong() : null;
        } catch (Exception e) {
            return null;
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
        // El Authentication puede traer authorities SCOPE_* además de ROLE_*:
        // se busca explícitamente el rol, no la primera authority.
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .findFirst()
                .orElse("ANONYMOUS");
    }
}