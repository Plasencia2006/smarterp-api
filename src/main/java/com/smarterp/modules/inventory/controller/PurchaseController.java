package com.smarterp.modules.inventory.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.inventory.entity.PurchaseOrder;
import com.smarterp.modules.inventory.entity.PurchaseStatus;
import com.smarterp.modules.inventory.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final UserContext userContext;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PurchaseOrder>>> getPurchases() {
        return ResponseEntity.ok(ApiResponse.success(purchaseOrderRepository.findAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseOrder>> createPurchase(@RequestBody PurchaseOrder purchaseOrder) {
        return ResponseEntity.ok(ApiResponse.success("Orden creada", purchaseOrderRepository.save(purchaseOrder)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrder>> updatePurchase(@PathVariable String id,
            @RequestBody PurchaseOrder purchaseOrder) {
        PurchaseOrder existing = purchaseOrderRepository.findById(id).orElseThrow();
        existing.setTotal(purchaseOrder.getTotal());
        existing.setStatus(purchaseOrder.getStatus());
        return ResponseEntity.ok(ApiResponse.success("Orden actualizada", purchaseOrderRepository.save(existing)));
    }

    @PostMapping("/{id}/receive")
    public ResponseEntity<ApiResponse<PurchaseOrder>> receiveMerchandise(@PathVariable String id) {
        PurchaseOrder purchaseOrder = purchaseOrderRepository.findById(id).orElseThrow();
        purchaseOrder.setStatus(PurchaseStatus.RECEIVED);
        return ResponseEntity
                .ok(ApiResponse.success("Mercancía recibida", purchaseOrderRepository.save(purchaseOrder)));
    }
}