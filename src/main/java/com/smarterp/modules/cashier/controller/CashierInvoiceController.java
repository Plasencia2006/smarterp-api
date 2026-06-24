package com.smarterp.modules.cashier.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.cashier.dto.*;
import com.smarterp.modules.cashier.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/cashier/invoices")
@RequiredArgsConstructor
@Slf4j
public class CashierInvoiceController {

    private final InvoiceService invoiceService;
    private final UserContext userContext;

    /**
     * 📋 LISTAR FACTURAS CON FILTROS
     * POST /cashier/invoices/search
     */
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<List<InvoiceSearchResponse>>> searchInvoices(
            @RequestBody InvoiceFilterRequest filter) {

        String businessId = userContext.getCurrentBusinessId();

        log.info("📋 Cajero buscando facturas con filtros: {}", filter);

        try {
            List<InvoiceSearchResponse> invoices = invoiceService.searchInvoices(businessId, filter);
            return ResponseEntity.ok(ApiResponse.success(invoices));
        } catch (Exception e) {
            log.error("❌ Error al buscar facturas: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 🔍 OBTENER DETALLE DE FACTURA
     * GET /cashier/invoices/{invoiceNumber}/detail
     */
    @GetMapping("/{invoiceNumber}/detail")
    public ResponseEntity<ApiResponse<InvoiceDetailResponse>> getInvoiceDetail(
            @PathVariable String invoiceNumber) {

        String businessId = userContext.getCurrentBusinessId();

        log.info("🔍 Obteniendo detalle de factura: {}", invoiceNumber);

        try {
            InvoiceDetailResponse detail = invoiceService.getInvoiceDetail(invoiceNumber, businessId);
            return ResponseEntity.ok(ApiResponse.success(detail));
        } catch (Exception e) {
            log.error("❌ Error al obtener detalle: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 🚫 ANULAR FACTURA
     * POST /cashier/invoices/{invoiceNumber}/void
     */
    @PostMapping("/{invoiceNumber}/void")
    public ResponseEntity<ApiResponse<InvoiceDetailResponse>> voidInvoice(
            @PathVariable String invoiceNumber,
            @RequestBody VoidInvoiceRequest request,
            Authentication authentication) {

        String businessId = userContext.getCurrentBusinessId();
        String userId = userContext.getCurrentUserId();
        String userName = getUserName(authentication);

        log.info("🚫 Anulando factura: {} - Motivo: {}", invoiceNumber, request.getReason());

        try {
            InvoiceDetailResponse result = invoiceService.voidInvoice(
                    invoiceNumber, businessId, userId, userName, request);
            return ResponseEntity.ok(ApiResponse.success("Factura anulada correctamente", result));
        } catch (Exception e) {
            log.error("❌ Error al anular factura: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 📊 RESUMEN DE VENTAS DEL DÍA
     * GET /cashier/invoices/summary?date=2026-06-21
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DailySalesSummaryResponse>> getDailySummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        String businessId = userContext.getCurrentBusinessId();
        LocalDate queryDate = date != null ? date : LocalDate.now();

        log.info(" Obteniendo resumen de ventas del día: {}", queryDate);

        try {
            DailySalesSummaryResponse summary = invoiceService.getDailySummary(businessId, queryDate);
            return ResponseEntity.ok(ApiResponse.success(summary));
        } catch (Exception e) {
            log.error("❌ Error al obtener resumen: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     *  VENTAS POR MÉTODO DE PAGO
     * GET
     * /cashier/invoices/by-payment-method?startDate=2026-06-01&endDate=2026-06-21
     */
    @GetMapping("/by-payment-method")
    public ResponseEntity<ApiResponse<List<SalesByPaymentMethodResponse>>> getSalesByPaymentMethod(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        String businessId = userContext.getCurrentBusinessId();
        LocalDate start = startDate != null ? startDate : LocalDate.now();
        LocalDate end = endDate != null ? endDate : start;

        log.info(" Obteniendo ventas por método de pago: {} a {}", start, end);

        try {
            List<SalesByPaymentMethodResponse> result = invoiceService.getSalesByPaymentMethod(
                    businessId, start, end);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error(" Error al obtener ventas por método: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     *  BUSCAR FACTURA POR NÚMERO
     * GET /cashier/invoices/find?number=F-20260621-0001
     */
    @GetMapping("/find")
    public ResponseEntity<ApiResponse<InvoiceSearchResponse>> findInvoiceByNumber(
            @RequestParam String number) {

        String businessId = userContext.getCurrentBusinessId();

        log.info(" Buscando factura por número: {}", number);

        try {
            InvoiceSearchResponse invoice = invoiceService.findInvoiceByNumber(number, businessId);
            return ResponseEntity.ok(ApiResponse.success(invoice));
        } catch (Exception e) {
            log.error(" Error al buscar factura: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 🔧 MÉTODO AUXILIAR: Obtener nombre del usuario
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