package com.digitalfix.bff.client;

import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.core.Authentication;

import com.digitalfix.bff.security.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProxyClient {

    private final RestTemplate restTemplate;

    public ResponseEntity<byte[]> forward(String targetUrl, HttpMethod method, HttpHeaders incomingHeaders, byte[] body, Authentication auth) {
        HttpHeaders headers = new HttpHeaders();
        // Copia content-type relevante
        String ct = incomingHeaders.getFirst(HttpHeaders.CONTENT_TYPE);
        if (ct != null) headers.set(HttpHeaders.CONTENT_TYPE, ct);
        headers.set(HttpHeaders.ACCEPT, "application/json");

        // Headers de identidad para downstream (mantiene compatibilidad con workorders que usaba X-User-*)
        headers.set("X-User-Email", SecurityUtils.extractEmail(auth));
        headers.set("X-User-Role", SecurityUtils.extractRole(auth));
        // Propaga Authorization si existe
        String authHeader = incomingHeaders.getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null) headers.set(HttpHeaders.AUTHORIZATION, authHeader);

        HttpEntity<byte[]> entity = new HttpEntity<>(body, headers);
        try {
            return restTemplate.exchange(targetUrl, method, entity, byte[].class);
        } catch (HttpStatusCodeException ex) {
            HttpHeaders respHeaders = new HttpHeaders();
            MediaType contentType = ex.getResponseHeaders() != null ? ex.getResponseHeaders().getContentType() : MediaType.APPLICATION_JSON;
            if (contentType != null) respHeaders.setContentType(contentType);
            return ResponseEntity.status(ex.getStatusCode()).headers(respHeaders).body(ex.getResponseBodyAsByteArray());
        }
    }
}
