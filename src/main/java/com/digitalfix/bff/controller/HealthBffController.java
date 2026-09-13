package com.digitalfix.bff.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthBffController {

    @GetMapping("/api/me")
    public ResponseEntity<Map<String, Object>> me(Authentication auth) {
        if (auth == null) return ResponseEntity.ok(Map.of("authenticated", false));
        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "principal", auth.getName(),
                "authorities", auth.getAuthorities().stream().map(Object::toString).toList()
        ));
    }
}
