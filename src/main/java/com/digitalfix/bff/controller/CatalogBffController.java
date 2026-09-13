package com.digitalfix.bff.controller;

import com.digitalfix.bff.client.ProxyClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CatalogBffController {

    private final ProxyClient proxy;

    @Value("${app.catalog-url:http://localhost:8082}")
    private String baseUrl;

    // Servicios
    @GetMapping("/api/catalog/servicios")
    public ResponseEntity<byte[]> listServicios(@RequestHeader HttpHeaders headers, @RequestParam(required = false) String soloActivos, Authentication auth) {
        String url = baseUrl + "/api/catalog/servicios" + (soloActivos != null ? "?soloActivos=" + soloActivos : "");
        return proxy.forward(url, HttpMethod.GET, headers, null, auth);
    }

    @GetMapping("/api/catalog/servicios/{id}")
    public ResponseEntity<byte[]> getServicio(@PathVariable String id, @RequestHeader HttpHeaders headers, Authentication auth) {
        return proxy.forward(baseUrl + "/api/catalog/servicios/" + id, HttpMethod.GET, headers, null, auth);
    }

    @PostMapping("/api/catalog/servicios")
    public ResponseEntity<byte[]> createServicio(@RequestHeader HttpHeaders headers, @RequestBody(required = false) byte[] body, Authentication auth) {
        return proxy.forward(baseUrl + "/api/catalog/servicios", HttpMethod.POST, headers, body, auth);
    }

    @PutMapping("/api/catalog/servicios/{id}")
    public ResponseEntity<byte[]> updateServicio(@PathVariable String id, @RequestHeader HttpHeaders headers, @RequestBody(required = false) byte[] body, Authentication auth) {
        return proxy.forward(baseUrl + "/api/catalog/servicios/" + id, HttpMethod.PUT, headers, body, auth);
    }

    @DeleteMapping("/api/catalog/servicios/{id}")
    public ResponseEntity<byte[]> deleteServicio(@PathVariable String id, @RequestHeader HttpHeaders headers, Authentication auth) {
        return proxy.forward(baseUrl + "/api/catalog/servicios/" + id, HttpMethod.DELETE, headers, null, auth);
    }

    // Repuestos
    @GetMapping("/api/catalog/repuestos")
    public ResponseEntity<byte[]> listRepuestos(@RequestHeader HttpHeaders headers, Authentication auth) {
        return proxy.forward(baseUrl + "/api/catalog/repuestos", HttpMethod.GET, headers, null, auth);
    }

    @GetMapping("/api/catalog/repuestos/stock-bajo")
    public ResponseEntity<byte[]> stockBajo(@RequestHeader HttpHeaders headers, Authentication auth) {
        return proxy.forward(baseUrl + "/api/catalog/repuestos/stock-bajo", HttpMethod.GET, headers, null, auth);
    }

    @GetMapping("/api/catalog/repuestos/{id}")
    public ResponseEntity<byte[]> getRepuesto(@PathVariable String id, @RequestHeader HttpHeaders headers, Authentication auth) {
        return proxy.forward(baseUrl + "/api/catalog/repuestos/" + id, HttpMethod.GET, headers, null, auth);
    }

    @PostMapping("/api/catalog/repuestos")
    public ResponseEntity<byte[]> createRepuesto(@RequestHeader HttpHeaders headers, @RequestBody(required = false) byte[] body, Authentication auth) {
        return proxy.forward(baseUrl + "/api/catalog/repuestos", HttpMethod.POST, headers, body, auth);
    }

    @PutMapping("/api/catalog/repuestos/{id}")
    public ResponseEntity<byte[]> updateRepuesto(@PathVariable String id, @RequestHeader HttpHeaders headers, @RequestBody(required = false) byte[] body, Authentication auth) {
        return proxy.forward(baseUrl + "/api/catalog/repuestos/" + id, HttpMethod.PUT, headers, body, auth);
    }

    @DeleteMapping("/api/catalog/repuestos/{id}")
    public ResponseEntity<byte[]> deleteRepuesto(@PathVariable String id, @RequestHeader HttpHeaders headers, Authentication auth) {
        return proxy.forward(baseUrl + "/api/catalog/repuestos/" + id, HttpMethod.DELETE, headers, null, auth);
    }

    @PostMapping("/api/catalog/repuestos/{id}/descontar")
    public ResponseEntity<byte[]> descontar(@PathVariable String id, @RequestHeader HttpHeaders headers, @RequestBody(required = false) byte[] body, Authentication auth) {
        return proxy.forward(baseUrl + "/api/catalog/repuestos/" + id + "/descontar", HttpMethod.POST, headers, body, auth);
    }

    @PostMapping("/api/catalog/repuestos/{id}/reponer")
    public ResponseEntity<byte[]> reponer(@PathVariable String id, @RequestHeader HttpHeaders headers, @RequestBody(required = false) byte[] body, Authentication auth) {
        return proxy.forward(baseUrl + "/api/catalog/repuestos/" + id + "/reponer", HttpMethod.POST, headers, body, auth);
    }

    @PostMapping("/api/catalog/repuestos/{id}/consumir")
    public ResponseEntity<byte[]> consumir(@PathVariable String id, @RequestHeader HttpHeaders headers, Authentication auth) {
        return proxy.forward(baseUrl + "/api/catalog/repuestos/" + id + "/consumir", HttpMethod.POST, headers, null, auth);
    }
}
