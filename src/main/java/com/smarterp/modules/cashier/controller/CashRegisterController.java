package com.smarterp.modules.cashier.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.cashier.dto.*;
import com.smarterp.modules.cashier.entity.CashRegister;
import com.smarterp.modules.cashier.entity.CashTransaction;
import com.smarterp.modules.cashier.service.CashRegisterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cashier")
@RequiredArgsConstructor
@Slf4j
public class CashRegisterController {

    private final CashRegisterService cashierService;
    private final UserContext userContext;

    /**
     * 🔍 BUSCAR COTIZACIÓN POR NÚMERO
     */
    @GetMapping("/quote/search")
    public ResponseEntity<ApiResponse<QuoteSearchResponse>> searchQuote(
            @RequestParam String number) {

        String businessId = userContext.getCurrentBusinessId();

        log.info("🔍 Cajero buscando cotización: {}", number);

        try {
            QuoteSearchResponse response = cashierService.searchQuote(number, businessId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("❌ Error al buscar cotización: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * ✅ VALIDAR COTIZACIÓN ANTES DE PAGAR
     */
    @PostMapping("/quote/validate")
    public ResponseEntity<ApiResponse<QuoteValidationResponse>> validateQuote(
            @RequestBody Map<String, Object> request) {

        String businessId = userContext.getCurrentBusinessId();
        String quoteNumber = (String) request.get("quoteNumber");

        @SuppressWarnings("unchecked")
        Map<String, List<String>> serialNumbers = (Map<String, List<String>>) request.get("serialNumbers");

        log.info("✅ Validando cotización: {}", quoteNumber);

        try {
            QuoteValidationResponse response = cashierService.validateQuote(
                    quoteNumber, businessId, serialNumbers);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 🟢 APERTURA DE CAJA
     */
    @PostMapping("/register/open")
    public ResponseEntity<ApiResponse<CashRegister>> openRegister(
            @RequestBody CashRegisterOpenRequest request,
            Authentication authentication) {

        String businessId = userContext.getCurrentBusinessId();
        String userId = userContext.getCurrentUserId();
        String userName = getUserName(authentication);

        log.info("🟢 Apertura de caja - Usuario: {} - Efectivo inicial: S/ {}", userName, request.getInitialCash());

        try {
            CashRegister register = cashierService.openRegister(businessId, userId, userName, request);
            return ResponseEntity.ok(ApiResponse.success("Caja abierta correctamente", register));
        } catch (Exception e) {
            log.error("❌ Error al abrir caja: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 🔴 CIERRE DE CAJA
     */
    @PostMapping("/register/{id}/close")
    public ResponseEntity<ApiResponse<CashRegister>> closeRegister(
            @PathVariable String id,
            @RequestBody CashRegisterCloseRequest request,
            Authentication authentication) {

        String businessId = userContext.getCurrentBusinessId();
        String userId = userContext.getCurrentUserId();

        log.info("🔴 Cierre de caja - Turno: {} - Efectivo final: S/ {}", id, request.getFinalCash());

        try {
            CashRegister register = cashierService.closeRegister(id, businessId, userId, request);
            return ResponseEntity.ok(ApiResponse.success("Caja cerrada correctamente", register));
        } catch (Exception e) {
            log.error("❌ Error al cerrar caja: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 💰 PROCESAR PAGO DE COTIZACIÓN
     */
    @PostMapping("/payment/process")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @RequestBody PaymentRequest request,
            Authentication authentication) {

        String businessId = userContext.getCurrentBusinessId();
        String userId = userContext.getCurrentUserId();
        String userName = getUserName(authentication);

        log.info("💰 Procesando pago - Cotización: {} - Método: {}", request.getQuoteNumber(),
                request.getPaymentMethod());

        try {
            PaymentResponse response = cashierService.processPayment(businessId, userId, userName, request);
            return ResponseEntity.ok(ApiResponse.success("Pago procesado correctamente", response));
        } catch (Exception e) {
            log.error("❌ Error al procesar pago: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 💸 REGISTRAR EGRESO
     */
    @PostMapping("/expense")
    public ResponseEntity<ApiResponse<CashTransaction>> registerExpense(
            @RequestParam String registerId,
            @RequestParam BigDecimal amount,
            @RequestParam String description,
            Authentication authentication) {

        String businessId = userContext.getCurrentBusinessId();
        String userId = userContext.getCurrentUserId();
        String userName = getUserName(authentication);

        log.info("💸 Registrando egreso - Turno: {} - Monto: S/ {} - Descripción: {}", registerId, amount, description);

        try {
            CashTransaction transaction = cashierService.registerExpense(
                    businessId, userId, userName, registerId, amount, description);
            return ResponseEntity.ok(ApiResponse.success("Egreso registrado correctamente", transaction));
        } catch (Exception e) {
            log.error("❌ Error al registrar egreso: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 📊 OBTENER TURNO ACTIVO
     */
    @GetMapping("/register/active")
    public ResponseEntity<ApiResponse<CashRegister>> getActiveRegister() {
        String userId = userContext.getCurrentUserId();
        CashRegister register = cashierService.getActiveRegister(userId);

        if (register == null) {
            return ResponseEntity.ok(ApiResponse.success("No hay turno activo", null));
        }
        return ResponseEntity.ok(ApiResponse.success(register));
    }

    /**
     * 📋 HISTORIAL DE TURNOS - ✅ UN SOLO MÉTODO (FILTRADO POR USUARIO ACTUAL)
     */
    @GetMapping("/register/history")
    public ResponseEntity<ApiResponse<List<CashRegister>>> getRegisterHistory() {
        String businessId = userContext.getCurrentBusinessId();
        String userId = userContext.getCurrentUserId(); // ✅ Obtener userId del contexto

        log.info("📋 Obteniendo historial de turnos - Business: {}, Cajero: {}", businessId, userId);

        // ✅ Filtrar SOLO los turnos del cajero actual
        List<CashRegister> registers = cashierService.getRegisterHistory(businessId, userId);

        log.info("✅ {} turnos encontrados para cajero {}", registers.size(), userId);

        return ResponseEntity.ok(ApiResponse.success(registers));
    }

    /**
     * 💵 TRANSACCIONES DEL TURNO
     */
    @GetMapping("/register/{id}/transactions")
    public ResponseEntity<ApiResponse<List<CashTransaction>>> getRegisterTransactions(
            @PathVariable String id) {

        List<CashTransaction> transactions = cashierService.getRegisterTransactions(id);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    /**
     * 📊 RESUMEN DEL TURNO
     */
    @GetMapping("/register/{id}/summary")
    public ResponseEntity<ApiResponse<CashRegisterSummaryResponse>> getRegisterSummary(
            @PathVariable String id) {

        try {
            CashRegisterSummaryResponse summary = cashierService.getRegisterSummary(id);
            return ResponseEntity.ok(ApiResponse.success(summary));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 📊 DASHBOARD DEL CAJERO
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        String businessId = userContext.getCurrentBusinessId();
        String userId = userContext.getCurrentUserId();

        Map<String, Object> dashboard = cashierService.getDashboard(businessId, userId);
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    /**
     * 🛒 VENTA DIRECTA (sin cotización previa)
     */
    @PostMapping("/sale/direct")
    public ResponseEntity<ApiResponse<PaymentResponse>> processDirectSale(
            @RequestBody DirectSaleRequest request,
            Authentication authentication) {

        String businessId = userContext.getCurrentBusinessId();
        String userId = userContext.getCurrentUserId();
        String userName = getUserName(authentication);

        log.info("🛒 Venta directa - Cliente: {} - Items: {}",
                request.getCustomerName(), request.getItems().size());

        try {
            PaymentResponse response = cashierService.processDirectSale(
                    businessId, userId, userName, request);
            return ResponseEntity.ok(ApiResponse.success(
                    "Venta procesada correctamente", response));
        } catch (Exception e) {
            log.error("❌ Error en venta directa: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 🔧 MÉTODO AUXILIAR
     */
    private String getUserName(Authentication authentication) {
        if (authentication == null) {
            return "Cajero";
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            return userDetails.getUsername();
        }

        return principal != null ? principal.toString() : "Cajero";
    }
}