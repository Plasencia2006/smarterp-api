package com.smarterp.modules.admin.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.admin.service.AdminCashService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/cash")
@RequiredArgsConstructor
@Slf4j
public class AdminCashController {

    private final AdminCashService adminCashService;
    private final UserContext userContext;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCashDashboard() {
        String businessId = userContext.getCurrentBusinessId();
        try {
            Map<String, Object> dashboard = adminCashService.getCashDashboard(businessId);
            return ResponseEntity.ok(ApiResponse.success(dashboard));
        } catch (Exception e) {
            log.error("❌ Error al obtener dashboard de cajas: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/registers")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCashRegistersHistory(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {

        String businessId = userContext.getCurrentBusinessId();
        try {
            Map<String, Object> result = adminCashService.getCashRegistersHistory(businessId, page, size);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("❌ Error al obtener historial de cajas: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/registers/{id}/transactions")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRegisterTransactions(
            @PathVariable String id) {
        try {
            Map<String, Object> result = adminCashService.getRegisterTransactions(id);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("❌ Error al obtener transacciones: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ✅ ARQUEOS PENDIENTES
    @GetMapping("/audits/pending")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPendingAudits() {
        String businessId = userContext.getCurrentBusinessId();
        try {
            Map<String, Object> result = adminCashService.getPendingAudits(businessId);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("❌ Error al obtener arqueos pendientes: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ✅ APROBAR ARQUEO
    @PostMapping("/audits/{id}/approve")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approveAudit(
            @PathVariable String id,
            @RequestParam(required = false) String notes) {

        String adminId = userContext.getCurrentUserId();
        try {
            Map<String, Object> result = adminCashService.approveAudit(id, adminId, notes);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("❌ Error al aprobar arqueo: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ✅ RECHAZAR ARQUEO
    @PostMapping("/audits/{id}/reject")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rejectAudit(
            @PathVariable String id,
            @RequestParam(required = false) String notes) {

        String adminId = userContext.getCurrentUserId();
        try {
            Map<String, Object> result = adminCashService.rejectAudit(id, adminId, notes);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("❌ Error al rechazar arqueo: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ✅ RETIROS PENDIENTES
    @GetMapping("/withdrawals/pending")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPendingWithdrawals() {
        String businessId = userContext.getCurrentBusinessId();
        try {
            Map<String, Object> result = adminCashService.getPendingWithdrawals(businessId);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("❌ Error al obtener retiros pendientes: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ✅ APROBAR RETIRO
    @PostMapping("/withdrawals/{id}/approve")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approveWithdrawal(
            @PathVariable String id,
            @RequestParam(required = false) String notes) {

        String adminId = userContext.getCurrentUserId();
        try {
            Map<String, Object> result = adminCashService.approveWithdrawal(id, adminId, notes);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("❌ Error al aprobar retiro: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ✅ RECHAZAR RETIRO
    @PostMapping("/withdrawals/{id}/reject")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rejectWithdrawal(
            @PathVariable String id,
            @RequestParam(required = false) String notes) {

        String adminId = userContext.getCurrentUserId();
        try {
            Map<String, Object> result = adminCashService.rejectWithdrawal(id, adminId, notes);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("❌ Error al rechazar retiro: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}