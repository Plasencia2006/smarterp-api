package com.smarterp.modules.admin.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.admin.service.AdminInventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/inventory")
@RequiredArgsConstructor
@Slf4j
public class AdminInventoryController {

    private final AdminInventoryService adminInventoryService;
    private final UserContext userContext;

    /**
     * 📊 DASHBOARD DE INVENTARIO
     * GET /admin/inventory/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInventoryDashboard() {
        String businessId = userContext.getCurrentBusinessId();

        log.info("📊 Admin obteniendo dashboard de inventario - Business: {}", businessId);

        try {
            Map<String, Object> dashboard = adminInventoryService.getInventoryDashboard(businessId);
            return ResponseEntity.ok(ApiResponse.success(dashboard));
        } catch (Exception e) {
            log.error("❌ Error al obtener dashboard de inventario: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 📋 LISTA DE PRODUCTOS CON STOCK
     * GET /admin/inventory/products
     */
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProductsWithStock(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {

        String businessId = userContext.getCurrentBusinessId();

        log.info("📋 Admin obteniendo productos - Business: {} - Page: {}", businessId, page);

        try {
            Map<String, Object> result = adminInventoryService.getProductsWithStock(businessId, page, size);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("❌ Error al obtener productos: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 📊 ESTADÍSTICAS POR CATEGORÍA
     * GET /admin/inventory/stats-by-category
     */
    @GetMapping("/stats-by-category")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatsByCategory() {
        String businessId = userContext.getCurrentBusinessId();

        log.info("📊 Admin obteniendo stats por categoría - Business: {}", businessId);

        try {
            Map<String, Object> stats = adminInventoryService.getStatsByCategory(businessId);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("❌ Error al obtener stats por categoría: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}