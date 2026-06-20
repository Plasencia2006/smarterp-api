package com.smarterp.modules.sales.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.sales.dto.SaleRequest;
import com.smarterp.modules.sales.entity.SalePOS;
import com.smarterp.modules.sales.service.SalePOSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sales/pos") // ✅ Endpoint único
@RequiredArgsConstructor
@Slf4j
public class SalePOSController {

    private final SalePOSService salePOSService;
    private final UserContext userContext;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SalePOS>>> getSales() {
        String businessId = userContext.getCurrentBusinessId();
        log.info("📋 Obteniendo ventas POS para business: {}", businessId);
        return ResponseEntity.ok(ApiResponse.success(salePOSService.getSalesByBusiness(businessId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SalePOS>> getSale(@PathVariable String id) {
        String businessId = userContext.getCurrentBusinessId();
        return ResponseEntity.ok(ApiResponse.success(salePOSService.getSaleById(id, businessId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SalePOS>> createSale(@RequestBody SaleRequest request) {
        String businessId = userContext.getCurrentBusinessId();
        log.info("🛒 Nueva venta POS - Business: {}", businessId);

        try {
            SalePOS salePOS = salePOSService.createSale(businessId, request);
            return ResponseEntity.ok(ApiResponse.success("Venta POS registrada", salePOS));
        } catch (Exception e) {
            log.error("❌ Error al crear venta POS: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<SalePOS>> cancelSale(@PathVariable String id) {
        String businessId = userContext.getCurrentBusinessId();
        log.info("❌ Cancelando venta POS: {} - Business: {}", id, businessId);

        try {
            SalePOS salePOS = salePOSService.cancelSale(id, businessId);
            return ResponseEntity.ok(ApiResponse.success("Venta POS cancelada", salePOS));
        } catch (Exception e) {
            log.error("❌ Error al cancelar venta POS: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}