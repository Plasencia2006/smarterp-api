package com.smarterp.modules.inventory.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.modules.inventory.entity.StockAlert;
import com.smarterp.modules.inventory.repository.StockAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory/alerts")
@RequiredArgsConstructor
public class StockAlertController {

    private final StockAlertRepository alertRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<StockAlert>>> getAlerts() {
        return ResponseEntity.ok(ApiResponse.success(alertRepository.findByIsAttendedFalse()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StockAlert>> markAsAttended(@PathVariable String id) {
        StockAlert alert = alertRepository.findById(id).orElseThrow();
        alert.setIsAttended(true);
        return ResponseEntity.ok(ApiResponse.success(alertRepository.save(alert)));
    }
}