package com.smarterp.modules.cashier.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.cashier.dto.*;
import com.smarterp.modules.cashier.service.CashWithdrawalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cashier/withdrawals")
@RequiredArgsConstructor
@Slf4j
public class CashWithdrawalController {

    private final CashWithdrawalService withdrawalService;
    private final UserContext userContext;

    /**
     *  SOLICITAR RETIRO
     * POST /cashier/withdrawals/request
     */
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<CashWithdrawalResponse>> requestWithdrawal(
            @RequestBody CashWithdrawalRequest request,
            Authentication authentication) {

        String businessId = userContext.getCurrentBusinessId();
        String cashierId = userContext.getCurrentUserId();
        String cashierName = getUserName(authentication);

        log.info("💸 Cajero {} solicitando retiro de S/ {}", cashierName, request.getAmount());

        try {
            CashWithdrawalResponse response = withdrawalService.requestWithdrawal(
                    businessId, request.getRegisterId(), cashierId, cashierName, request);
            return ResponseEntity.ok(ApiResponse.success("Retiro solicitado", response));
        } catch (Exception e) {
            log.error("❌ Error al solicitar retiro: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     *  APROBAR RETIRO (supervisor)
     * POST /cashier/withdrawals/{id}/approve
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<CashWithdrawalResponse>> approveWithdrawal(
            @PathVariable String id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        String businessId = userContext.getCurrentBusinessId();
        String supervisorId = userContext.getCurrentUserId();
        String supervisorName = getUserName(authentication);
        String approvalNotes = request.get("approvalNotes");

        log.info("✅ Supervisor {} aprobando retiro: {}", supervisorName, id);

        try {
            CashWithdrawalResponse response = withdrawalService.approveWithdrawal(
                    id, businessId, supervisorId, supervisorName, approvalNotes);
            return ResponseEntity.ok(ApiResponse.success("Retiro aprobado", response));
        } catch (Exception e) {
            log.error("❌ Error al aprobar retiro: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     *  RECHAZAR RETIRO
     * POST /cashier/withdrawals/{id}/reject
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<CashWithdrawalResponse>> rejectWithdrawal(
            @PathVariable String id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        String businessId = userContext.getCurrentBusinessId();
        String supervisorId = userContext.getCurrentUserId();
        String supervisorName = getUserName(authentication);
        String rejectionNotes = request.get("rejectionNotes");

        try {
            CashWithdrawalResponse response = withdrawalService.rejectWithdrawal(
                    id, businessId, supervisorId, supervisorName, rejectionNotes);
            return ResponseEntity.ok(ApiResponse.success("Retiro rechazado", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     *  COMPLETAR RETIRO (entregar dinero a caja fuerte)
     * POST /cashier/withdrawals/{id}/complete
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<CashWithdrawalResponse>> completeWithdrawal(
            @PathVariable String id,
            Authentication authentication) {

        String businessId = userContext.getCurrentBusinessId();
        String userId = userContext.getCurrentUserId();
        String userName = getUserName(authentication);

        log.info("📦 Completando retiro: {}", id);

        try {
            CashWithdrawalResponse response = withdrawalService.completeWithdrawal(
                    id, businessId, userId, userName);
            return ResponseEntity.ok(ApiResponse.success("Retiro completado", response));
        } catch (Exception e) {
            log.error("❌ Error al completar retiro: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     *  LISTAR RETIROS DE UN TURNO
     * GET /cashier/withdrawals?registerId=xxx
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CashWithdrawalResponse>>> getWithdrawals(
            @RequestParam String registerId) {

        String businessId = userContext.getCurrentBusinessId();

        try {
            List<CashWithdrawalResponse> withdrawals = withdrawalService.getWithdrawalsByRegister(registerId,
                    businessId);
            return ResponseEntity.ok(ApiResponse.success(withdrawals));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     *  DETALLE DE UN RETIRO
     * GET /cashier/withdrawals/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CashWithdrawalResponse>> getWithdrawalDetail(
            @PathVariable String id) {

        String businessId = userContext.getCurrentBusinessId();

        try {
            CashWithdrawalResponse withdrawal = withdrawalService.getWithdrawalDetail(id, businessId);
            return ResponseEntity.ok(ApiResponse.success(withdrawal));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     *  RESUMEN DE FLUJO DE EFECTIVO
     * GET /cashier/withdrawals/cash-flow?registerId=xxx
     */
    @GetMapping("/cash-flow")
    public ResponseEntity<ApiResponse<CashFlowSummaryResponse>> getCashFlowSummary(
            @RequestParam String registerId) {

        String businessId = userContext.getCurrentBusinessId();

        try {
            CashFlowSummaryResponse summary = withdrawalService.getCashFlowSummary(registerId, businessId);
            return ResponseEntity.ok(ApiResponse.success(summary));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 🔍 VERIFICAR SI SUPERA LÍMITE
     * GET /cashier/withdrawals/exceeds-limit?registerId=xxx
     */
    @GetMapping("/exceeds-limit")
    public ResponseEntity<ApiResponse<Boolean>> exceedsCashLimit(@RequestParam String registerId) {
        String businessId = userContext.getCurrentBusinessId();

        try {
            boolean exceeds = withdrawalService.exceedsCashLimit(registerId, businessId);
            return ResponseEntity.ok(ApiResponse.success(exceeds));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private String getUserName(Authentication authentication) {
        if (authentication == null)
            return "Usuario";
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return principal != null ? principal.toString() : "Usuario";
    }
}