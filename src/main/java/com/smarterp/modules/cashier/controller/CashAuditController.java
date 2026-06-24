package com.smarterp.modules.cashier.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.cashier.dto.*;
import com.smarterp.modules.cashier.service.CashAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cashier/audits")
@RequiredArgsConstructor
@Slf4j
public class CashAuditController {

    private final CashAuditService auditService;
    private final UserContext userContext;

    /**
     * 🔍 INICIAR ARQUEO PARCIAL
     * POST /cashier/audits/start
     */
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<CashAuditResponse>> startAudit(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        String businessId = userContext.getCurrentBusinessId();
        String supervisorId = userContext.getCurrentUserId();
        String supervisorName = getUserName(authentication);

        String registerId = request.get("registerId");
        String auditType = request.getOrDefault("auditType", "PARCIAL");

        log.info("🔍 Supervisor {} iniciando arqueo {} en turno {}",
                supervisorName, auditType, registerId);

        try {
            CashAuditResponse response = auditService.startAudit(
                    businessId, registerId, supervisorId, supervisorName, auditType);
            return ResponseEntity.ok(ApiResponse.success("Arqueo iniciado correctamente", response));
        } catch (Exception e) {
            log.error("❌ Error al iniciar arqueo: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * ✅ COMPLETAR ARQUEO (ingresar conteo)
     * POST /cashier/audits/{id}/complete
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<CashAuditResponse>> completeAudit(
            @PathVariable String id,
            @RequestBody CashAuditRequest request,
            Authentication authentication) {

        String businessId = userContext.getCurrentBusinessId();
        String supervisorId = userContext.getCurrentUserId();

        log.info("✅ Supervisor completando arqueo: {}", id);

        try {
            CashAuditResponse response = auditService.completeAudit(
                    id, businessId, supervisorId, request);
            return ResponseEntity.ok(ApiResponse.success("Arqueo completado", response));
        } catch (Exception e) {
            log.error("❌ Error al completar arqueo: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * ❌ CANCELAR ARQUEO
     * POST /cashier/audits/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<CashAuditResponse>> cancelAudit(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {

        String businessId = userContext.getCurrentBusinessId();
        String reason = request.get("reason");

        try {
            CashAuditResponse response = auditService.cancelAudit(id, businessId, reason);
            return ResponseEntity.ok(ApiResponse.success("Arqueo cancelado", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 📋 LISTAR ARQUEOS DE UN TURNO
     * GET /cashier/audits?registerId=xxx
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CashAuditResponse>>> getAuditsByRegister(
            @RequestParam String registerId) {

        String businessId = userContext.getCurrentBusinessId();

        try {
            List<CashAuditResponse> audits = auditService.getAuditsByRegister(registerId, businessId);
            return ResponseEntity.ok(ApiResponse.success(audits));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 🔍 OBTENER DETALLE DE UN ARQUEO
     * GET /cashier/audits/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CashAuditResponse>> getAuditDetail(@PathVariable String id) {
        String businessId = userContext.getCurrentBusinessId();

        try {
            CashAuditResponse audit = auditService.getAuditDetail(id, businessId);
            return ResponseEntity.ok(ApiResponse.success(audit));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private String getUserName(Authentication authentication) {
        if (authentication == null)
            return "Supervisor";
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return principal != null ? principal.toString() : "Supervisor";
    }
}