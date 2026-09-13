package com.digitalfix.bff.controller;

import com.digitalfix.bff.client.ProxyClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workorders")
@RequiredArgsConstructor
@Slf4j
public class WorkOrderBffController {

    private final ProxyClient proxy;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.workorders-url:http://localhost:8081}")
    private String baseUrl;

    @Value("${app.catalog-url:http://localhost:8082}")
    private String catalogUrl;

    @GetMapping
    public ResponseEntity<byte[]> list(@RequestHeader HttpHeaders headers, Authentication auth) {
        return proxy.forward(baseUrl + "/api/workorders", HttpMethod.GET, headers, null, auth);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> get(@PathVariable String id, @RequestHeader HttpHeaders headers, Authentication auth) {
        return proxy.forward(baseUrl + "/api/workorders/" + id, HttpMethod.GET, headers, null, auth);
    }

    @PostMapping
    public ResponseEntity<byte[]> create(@RequestHeader HttpHeaders headers, @RequestBody(required = false) byte[] body, Authentication auth) {
        return proxy.forward(baseUrl + "/api/workorders", HttpMethod.POST, headers, body, auth);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<byte[]> changeStatus(@PathVariable String id, @RequestHeader HttpHeaders headers, @RequestBody(required = false) byte[] body, Authentication auth) {
        ResponseEntity<byte[]> resp = proxy.forward(baseUrl + "/api/workorders/" + id + "/status", HttpMethod.PATCH, headers, body, auth);
        if (!resp.getStatusCode().is2xxSuccessful() || body == null) return resp;

        try {
            JsonNode reqNode = objectMapper.readTree(body);
            String targetEstado = reqNode.path("estado").asText("");
            if (!"ASIGNADA".equalsIgnoreCase(targetEstado)) return resp;

            // Extrae repuestoId de la respuesta de workorders (si tiene)
            Long repuestoId = null;
            if (resp.getBody() != null) {
                JsonNode respNode = objectMapper.readTree(resp.getBody());
                if (respNode.has("repuestoId") && !respNode.get("repuestoId").isNull()) {
                    repuestoId = respNode.get("repuestoId").asLong();
                }
            }
            if (repuestoId == null) return resp;

            log.info("Orquestacion stock: workorder {} ASIGNADA con repuesto {} -> consumir 1 unidad", id, repuestoId);
            ResponseEntity<byte[]> stockResp = proxy.forward(catalogUrl + "/api/catalog/repuestos/" + repuestoId + "/consumir", HttpMethod.POST, headers, null, auth);
            if (!stockResp.getStatusCode().is2xxSuccessful()) {
                log.warn("Fallo al descontar stock repuesto {} para orden {}: {} {}", repuestoId, id, stockResp.getStatusCode(), stockResp.getBody() != null ? new String(stockResp.getBody()) : "");
                // No revierte estado, pero informa al cliente que stock fallo (se podria revertir a CREADA si se requiere transaccionalidad critica)
                // Se propaga warning via header
                HttpHeaders merged = new HttpHeaders();
                if (resp.getHeaders() != null) merged.putAll(resp.getHeaders());
                merged.set("X-Stock-Warning", "Stock no descontado: " + (stockResp.getBody() != null ? new String(stockResp.getBody()) : stockResp.getStatusCode().toString()));
                return ResponseEntity.status(resp.getStatusCode()).headers(merged).body(resp.getBody());
            }
            log.info("Stock descontado OK repuesto {} para orden {}", repuestoId, id);
        } catch (Exception e) {
            log.error("Error en orquestacion stock para orden {}: {}", id, e.getMessage());
        }
        return resp;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<byte[]> delete(@PathVariable String id, @RequestHeader HttpHeaders headers, Authentication auth) {
        return proxy.forward(baseUrl + "/api/workorders/" + id, HttpMethod.DELETE, headers, null, auth);
    }
}
