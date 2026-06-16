package com.smarterp.modules.accounting.controller;

import com.smarterp.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/accounting/reconciliation")
public class ReconciliationController {

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> startReconciliation(
            @RequestBody Map<String, Object> request) {
        Map<String, Object> reconciliation = new HashMap<>();
        reconciliation.put("id", java.util.UUID.randomUUID().toString());
        reconciliation.put("status", "IN_PROGRESS");
        return ResponseEntity.ok(ApiResponse.success(reconciliation));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReconciliation(@PathVariable String id) {
        Map<String, Object> reconciliation = new HashMap<>();
        reconciliation.put("id", id);
        reconciliation.put("status", "COMPLETED");
        return ResponseEntity.ok(ApiResponse.success(reconciliation));
    }
}