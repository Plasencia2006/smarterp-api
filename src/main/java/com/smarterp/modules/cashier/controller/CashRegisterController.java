package com.smarterp.modules.cashier.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.cashier.dto.CashRegisterCloseRequest;
import com.smarterp.modules.cashier.dto.CashRegisterOpenRequest;
import com.smarterp.modules.cashier.dto.CashRegisterSummaryResponse;
import com.smarterp.modules.cashier.entity.CashRegister;
import com.smarterp.modules.cashier.service.CashRegisterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cashier/register")
@RequiredArgsConstructor
public class CashRegisterController {

    private final CashRegisterService cashRegisterService;
    private final UserContext userContext;

    @PostMapping("/open")
    public ResponseEntity<ApiResponse<CashRegister>> openCashRegister(
            @Valid @RequestBody CashRegisterOpenRequest request) {

        String businessId = userContext.getCurrentBusinessId();
        String userId = userContext.getCurrentUserId();

        CashRegister cashRegister = cashRegisterService.openCashRegister(businessId, userId, request);

        return ResponseEntity.ok(ApiResponse.success("Caja abierta correctamente", cashRegister));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<CashRegister>> getStatus() {
        String businessId = userContext.getCurrentBusinessId();
        CashRegister cashRegister = cashRegisterService.getOpenCashRegister(businessId);
        return ResponseEntity.ok(ApiResponse.success(cashRegister));
    }

    @PostMapping("/close")
    public ResponseEntity<ApiResponse<CashRegister>> closeCashRegister(
            @Valid @RequestBody CashRegisterCloseRequest request) {

        String businessId = userContext.getCurrentBusinessId();
        String userId = userContext.getCurrentUserId();

        CashRegister cashRegister = cashRegisterService.closeCashRegister(
                request.getId(), businessId, userId, request);

        return ResponseEntity.ok(ApiResponse.success("Caja cerrada correctamente", cashRegister));
    }

    @GetMapping("/{id}/summary")
    public ResponseEntity<ApiResponse<CashRegisterSummaryResponse>> getSummary(
            @PathVariable String id) {

        String businessId = userContext.getCurrentBusinessId();
        CashRegisterSummaryResponse summary = cashRegisterService.getSummary(id, businessId);

        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}