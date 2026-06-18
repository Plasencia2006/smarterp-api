package com.smarterp.modules.inventory.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.inventory.entity.StockAlert;
import com.smarterp.modules.inventory.repository.StockAlertRepository;
import com.smarterp.modules.inventory.service.StockAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/inventory/alerts")
@RequiredArgsConstructor
@Slf4j
public class StockAlertController {

    private final StockAlertRepository alertRepository;
    private final StockAlertService stockAlertService;
    private final UserContext userContext;

    /**
     * ✅ Generar alertas manualmente (para testing o refresh)
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<Integer>> generateAlerts() {
        String businessId = userContext.getCurrentBusinessId();
        int count = stockAlertService.generateAllAlerts(businessId);
        return ResponseEntity.ok(ApiResponse.success("Alertas generadas", count));
    }

    /**
     * ✅ Obtener todas las alertas con estadísticas
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAlerts() {
        String businessId = userContext.getCurrentBusinessId();

        List<StockAlert> pendingAlerts = alertRepository.findByBusinessIdAndIsAttended(businessId, false);
        List<StockAlert> attendedAlerts = alertRepository.findByBusinessIdAndIsAttended(businessId, true);
        List<StockAlert> allAlerts = alertRepository.findByBusinessIdOrderByCreatedAtDesc(businessId);

        Map<String, Object> response = new HashMap<>();
        response.put("pending", pendingAlerts);
        response.put("attended", attendedAlerts);
        response.put("all", allAlerts);

        Map<String, Long> stats = new HashMap<>();
        stats.put("pending", (long) pendingAlerts.size());
        stats.put("attended", (long) attendedAlerts.size());
        stats.put("total", (long) allAlerts.size());

        response.put("stats", stats);

        return ResponseEntity.ok(ApiResponse.success("Alertas obtenidas", response));
    }

    /**
     * ✅ Marcar alerta como atendida
     */
    @PutMapping("/{id}/attend")
    public ResponseEntity<ApiResponse<StockAlert>> attendAlert(@PathVariable String id) {
        String businessId = userContext.getCurrentBusinessId();

        StockAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alerta no encontrada"));

        if (!alert.getBusinessId().equals(businessId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("No tiene permisos"));
        }

        alert.setIsAttended(true);
        alert.setAttendedAt(LocalDateTime.now());
        StockAlert updated = alertRepository.save(alert);

        return ResponseEntity.ok(ApiResponse.success("Alerta atendida", updated));
    }

    /**
     * ✅ Eliminar alerta
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAlert(@PathVariable String id) {
        String businessId = userContext.getCurrentBusinessId();

        StockAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alerta no encontrada"));

        if (!alert.getBusinessId().equals(businessId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("No tiene permisos"));
        }

        alertRepository.delete(alert);
        return ResponseEntity.ok(ApiResponse.success("Alerta eliminada", null));
    }
}