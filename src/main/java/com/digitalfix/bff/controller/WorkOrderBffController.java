package com.digitalfix.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.digitalfix.bff.client.ProxyClient;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/workorders")
@RequiredArgsConstructor
public class WorkOrderBffController {

    private final ProxyClient proxy;

    @Value("${app.workorders-url:http://localhost:8081}")
    private String baseUrl;

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
        return proxy.forward(baseUrl + "/api/workorders/" + id + "/status", HttpMethod.PATCH, headers, body, auth);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<byte[]> delete(@PathVariable String id, @RequestHeader HttpHeaders headers, Authentication auth) {
        return proxy.forward(baseUrl + "/api/workorders/" + id, HttpMethod.DELETE, headers, null, auth);
    }
}
