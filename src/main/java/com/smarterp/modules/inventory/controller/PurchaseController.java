package com.smarterp.modules.inventory.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.inventory.dto.PurchaseOrderRequest;
import com.smarterp.modules.inventory.entity.PurchaseOrder;
import com.smarterp.modules.inventory.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory/purchases")
@RequiredArgsConstructor
@Slf4j
public class PurchaseController {

    private final PurchaseOrderService purchaseOrderService;
    private final UserContext userContext;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PurchaseOrder>>> getPurchases() {
        String businessId = userContext.getCurrentBusinessId();
        log.info("🔍 GET /inventory/purchases - Business: {}", businessId);

        List<PurchaseOrder> orders = purchaseOrderService.getOrdersByBusiness(businessId);

        log.info("✅ Retornando {} órdenes", orders.size());
        if (!orders.isEmpty()) {
            PurchaseOrder first = orders.get(0);
            log.info("📦 Primera orden: {} - Items: {} - Total: {}",
                    first.getOrderNumber(),
                    first.getItems() != null ? first.getItems().size() : 0,
                    first.getTotal());
        }

        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrder>> getPurchase(@PathVariable String id) {
        String businessId = userContext.getCurrentBusinessId();
        log.info("🔍 GET /inventory/purchases/{} - Business: {}", id, businessId);

        PurchaseOrder order = purchaseOrderService.getOrderById(id, businessId);

        log.info("✅ Orden encontrada: {} - Items: {}",
                order.getOrderNumber(),
                order.getItems() != null ? order.getItems().size() : 0);

        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseOrder>> createPurchase(
            @RequestBody PurchaseOrderRequest request) {
        String businessId = userContext.getCurrentBusinessId();
        log.info("➕ POST /inventory/purchases - Business: {}", businessId);
        log.info("📝 Request: supplier={}, items={}",
                request.getSupplierName(),
                request.getItems() != null ? request.getItems().size() : 0);

        PurchaseOrder created = purchaseOrderService.createOrder(businessId, request);

        log.info("✅ Orden creada exitosamente: {}", created.getId());

        return ResponseEntity.ok(ApiResponse.success("Orden creada", created));
    }

    /**
     * 🚚 ENDPOINT MÁGICO: Recibir mercadería y actualizar stock
     */
    @PostMapping("/{id}/receive")
    public ResponseEntity<ApiResponse<PurchaseOrder>> receiveMerchandise(@PathVariable String id) {
        String businessId = userContext.getCurrentBusinessId();

        log.info("🚚 POST /inventory/purchases/{}/receive", id);
        log.info("🏢 Business ID del contexto: {}", businessId);

        try {
            PurchaseOrder updated = purchaseOrderService.receiveMerchandise(id, businessId);

            log.info("✅ Orden recibida exitosamente: {}", updated.getId());

            return ResponseEntity.ok(ApiResponse.success(
                    "✅ Mercancía recibida. Stock actualizado automáticamente.",
                    updated));

        } catch (Exception e) {
            log.error("❌ Error al recibir mercancía: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("Error al recibir mercancía: " + e.getMessage()));
        }
    }
}