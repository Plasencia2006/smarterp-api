package com.smarterp.modules.cashier.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.cashier.dto.*;
import com.smarterp.modules.cashier.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cashier/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;
    private final UserContext userContext;

    @PostMapping
    public ResponseEntity<ApiResponse<SaleResponse>> createSale(
            @Valid @RequestBody SaleRequest request) {

        String businessId = userContext.getCurrentBusinessId();
        String userId = userContext.getCurrentUserId();

        SaleResponse sale = saleService.createSale(businessId, userId, request);
        return ResponseEntity.ok(ApiResponse.success("Venta registrada", sale));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SaleResponse>>> getMySales() {
        String businessId = userContext.getCurrentBusinessId();
        String userId = userContext.getCurrentUserId();

        List<SaleResponse> sales = saleService.getMySales(businessId, userId);
        return ResponseEntity.ok(ApiResponse.success(sales));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SaleResponse>> getSale(@PathVariable String id) {
        String businessId = userContext.getCurrentBusinessId();
        SaleResponse sale = saleService.getSaleById(id, businessId);
        return ResponseEntity.ok(ApiResponse.success(sale));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<SaleResponse>> cancelSale(@PathVariable String id) {
        String businessId = userContext.getCurrentBusinessId();
        SaleResponse sale = saleService.cancelSale(id, businessId);
        return ResponseEntity.ok(ApiResponse.success("Venta cancelada", sale));
    }
}