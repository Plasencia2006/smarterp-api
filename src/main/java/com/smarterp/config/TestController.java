package com.smarterp.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "SMART ERP API está funcionando correctamente");
        response.put("timestamp", LocalDateTime.now());
        response.put("version", "1.0.0");
        response.put("modules", new String[] {
                "Cajero",
                "Vendedor",
                "Inventario",
                "Contador",
                "Soporte"
        });
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "smarterp-api");
        return ResponseEntity.ok(response);
    }
}