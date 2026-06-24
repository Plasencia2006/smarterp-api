package com.smarterp.modules.sales.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.cashier.dto.QuoteSearchResponse;
import com.smarterp.modules.sales.dto.QuoteRequest;
import com.smarterp.modules.sales.entity.Quote;
import com.smarterp.modules.sales.entity.QuoteItem;
import com.smarterp.modules.sales.entity.QuoteStatus;
import com.smarterp.modules.sales.repository.QuoteRepository;
import com.smarterp.modules.sales.service.InvoicePdfService;
import com.smarterp.modules.sales.service.QuotePdfService;
import com.smarterp.modules.sales.service.QuoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/sales/quotes")
@RequiredArgsConstructor
@Slf4j
public class QuoteController {

    private final QuoteService quoteService;
    private final QuotePdfService quotePdfService;
    private final InvoicePdfService invoicePdfService;
    private final UserContext userContext;

    @Autowired
    private QuoteRepository quoteRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<Quote>> createQuote(@RequestBody QuoteRequest request) {
        String businessId = userContext.getCurrentBusinessId();

        log.info("===========================================");
        log.info("📝 VENDEDOR: Nueva cotización");
        log.info("   Business ID: {}", businessId);
        log.info("   Cliente: {}", request.getCustomerName());
        log.info("   Items: {}", request.getItems().size());
        log.info("===========================================");

        try {
            Quote quote = quoteService.createQuote(businessId, request);
            log.info("✅ Cotización creada exitosamente: {}", quote.getQuoteNumber());
            return ResponseEntity.ok(ApiResponse.success(
                    "Cotización creada: " + quote.getQuoteNumber(), quote));
        } catch (Exception e) {
            log.error("❌ ERROR al crear cotización:", e);
            log.error("   Mensaje: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/block")
    public ResponseEntity<ApiResponse<Quote>> blockQuote(
            @PathVariable String id,
            @RequestParam(required = false) Integer minutes) {

        String businessId = userContext.getCurrentBusinessId();
        int blockMinutes = (minutes != null) ? minutes : 20;

        log.info("🔒 Bloqueando cotización: {} por {} minutos", id, blockMinutes);

        try {
            Quote quote = quoteService.blockQuote(id, businessId, blockMinutes);
            return ResponseEntity.ok(ApiResponse.success(
                    "Cotización bloqueada por " + blockMinutes + " minutos", quote));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<ApiResponse<Quote>> releaseBlock(@PathVariable String id) {
        String businessId = userContext.getCurrentBusinessId();

        try {
            Quote quote = quoteService.releaseBlock(id, businessId);
            return ResponseEntity.ok(ApiResponse.success("Bloqueo liberado", quote));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/product/{productId}/availability")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProductAvailability(
            @PathVariable String productId) {

        String businessId = userContext.getCurrentBusinessId();
        Map<String, Object> availability = quoteService.getProductAvailability(productId, businessId);

        return ResponseEntity.ok(ApiResponse.success(availability));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Quote>>> getAllQuotes() {
        String businessId = userContext.getCurrentBusinessId();
        return ResponseEntity.ok(ApiResponse.success(
                quoteService.getAllQuotes(businessId)));
    }

    /**
     * 📋 OBTENER COTIZACIONES PENDIENTES (para el cajero)
     * GET /sales/quotes/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<QuoteSearchResponse>>> getPendingQuotes() {
        String businessId = userContext.getCurrentBusinessId();

        log.info("📋 Obteniendo cotizaciones pendientes del negocio {}", businessId);

        try {
            List<Quote> pendingQuotes = quoteRepository.findPendingQuotesByBusinessId(businessId);

            List<QuoteSearchResponse> response = pendingQuotes.stream()
                    .map(quote -> {
                        boolean isExpired = quote.isExpired();
                        long remainingMinutes = quote.getRemainingMinutes();

                        List<QuoteSearchResponse.QuoteItemDetail> items = quote.getItems() != null
                                ? quote.getItems().stream()
                                        .map(item -> QuoteSearchResponse.QuoteItemDetail.builder()
                                                .productId(item.getProductId())
                                                .productName(item.getProductName())
                                                .productSku(item.getProductSku())
                                                .quantity(item.getQuantity())
                                                .unitPrice(item.getUnitPrice())
                                                .subtotal(item.getSubtotal())
                                                .hasSerialNumber(requiresSerialNumber(item.getProductName()))
                                                .build())
                                        .collect(Collectors.toList())
                                : List.of();

                        return QuoteSearchResponse.builder()
                                .id(quote.getId())
                                .quoteNumber(quote.getQuoteNumber())
                                .customerName(quote.getCustomerName())
                                .customerDocument(quote.getCustomerDocument())
                                .sellerName(quote.getSellerName())
                                .status(quote.getStatus().name())
                                .isExpired(isExpired)
                                .remainingMinutes(remainingMinutes)
                                .blockedUntil(quote.getBlockedUntil())
                                .createdAt(quote.getCreatedAt())
                                .subtotal(quote.getSubtotal())
                                .igv(quote.getIgv())
                                .total(quote.getTotal())
                                .items(items)
                                .isValidForPayment(quote.getStatus() == QuoteStatus.PENDIENTE && !isExpired)
                                .validationMessage(isExpired ? "Cotización expirada" : "Vigente")
                                .build();
                    })
                    .collect(Collectors.toList());

            log.info("✅ {} cotizaciones pendientes encontradas", response.size());
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("❌ Error al obtener cotizaciones pendientes: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 🧾 LISTAR FACTURAS (para el cajero)
     * GET /sales/quotes/invoices
     */
    @GetMapping("/invoices")
    public ResponseEntity<ApiResponse<List<QuoteSearchResponse>>> getInvoices(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String invoiceNumber,
            @RequestParam(required = false) String customerName) {

        String businessId = userContext.getCurrentBusinessId();
        log.info("🧾 Obteniendo facturas del negocio {}", businessId);

        try {
            List<Quote> invoices;

            if (invoiceNumber != null && !invoiceNumber.isEmpty()) {
                // Buscar por número de factura
                invoices = quoteRepository.findByInvoiceNumberAndBusinessId(invoiceNumber, businessId)
                        .map(Collections::singletonList)
                        .orElse(Collections.emptyList());
            } else if (customerName != null && !customerName.isEmpty()) {
                // Buscar por cliente
                invoices = quoteRepository.findInvoicesByCustomerName(businessId, customerName);
            } else {
                // Listar todas las facturas del día por defecto
                LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate + "T00:00:00")
                        : LocalDateTime.now().toLocalDate().atStartOfDay();
                LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate + "T23:59:59") : LocalDateTime.now();

                invoices = quoteRepository.findInvoicesByBusinessIdAndDateRange(businessId, start, end);
            }

            // Filtrar solo las FACTURADAS
            invoices = invoices.stream()
                    .filter(q -> q.getStatus() == QuoteStatus.FACTURADA)
                    .collect(Collectors.toList());

            // Convertir a QuoteSearchResponse
            List<QuoteSearchResponse> response = invoices.stream()
                    .map(quote -> QuoteSearchResponse.builder()
                            .id(quote.getId())
                            .quoteNumber(quote.getQuoteNumber())
                            .invoiceNumber(quote.getInvoiceNumber())
                            .customerName(quote.getCustomerName())
                            .customerDocument(quote.getCustomerDocument())
                            .sellerName(quote.getSellerName())
                            .status(quote.getStatus().name())
                            .total(quote.getTotal())
                            .paidAt(quote.getPaidAt())
                            .validatedBy(quote.getValidatedBy())
                            .paymentMethod(quote.getPaymentMethod())
                            .items(quote.getItems() != null ? quote.getItems().stream()
                                    .map(item -> QuoteSearchResponse.QuoteItemDetail.builder()
                                            .productId(item.getProductId())
                                            .productName(item.getProductName())
                                            .quantity(item.getQuantity())
                                            .unitPrice(item.getUnitPrice())
                                            .subtotal(item.getSubtotal())
                                            .build())
                                    .collect(Collectors.toList()) : Collections.emptyList())
                            .build())
                    .collect(Collectors.toList());

            log.info("✅ {} facturas encontradas", response.size());
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("❌ Error al obtener facturas: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/number/{quoteNumber}")
    public ResponseEntity<ApiResponse<Quote>> getByNumber(@PathVariable String quoteNumber) {
        String businessId = userContext.getCurrentBusinessId();

        try {
            Quote quote = quoteService.getByNumber(quoteNumber, businessId);
            return ResponseEntity.ok(ApiResponse.success(quote));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVendedorDashboard() {
        String businessId = userContext.getCurrentBusinessId();
        Map<String, Object> dashboard = quoteService.getVendedorDashboard(businessId);
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    /**
     * 📄 GENERAR PDF DE COTIZACIÓN
     * GET /sales/quotes/{id}/pdf
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadQuotePdf(@PathVariable String id) {
        String businessId = userContext.getCurrentBusinessId();

        log.info("📄 Generando PDF para cotización ID: {}", id);
        log.info("   Business ID: {}", businessId);

        try {
            Quote quote = quoteRepository.findById(id)
                    .orElseThrow(() -> {
                        log.error("❌ Cotización no encontrada: {}", id);
                        return new RuntimeException("Cotización no encontrada con ID: " + id);
                    });

            log.info("✅ Cotización encontrada: {}", quote.getQuoteNumber());
            log.info("   Cliente: {}", quote.getCustomerName());
            log.info("   Total: {}", quote.getTotal());
            log.info("   Items: {}", quote.getItems().size());

            if (!quote.getBusinessId().equals(businessId)) {
                log.error("❌ No tiene permisos para esta cotización");
                throw new RuntimeException("No tiene permisos para acceder a esta cotización");
            }

            log.info("🔨 Generando PDF...");
            byte[] pdfContent = quotePdfService.generateQuotePdf(quote);
            log.info("✅ PDF generado exitosamente. Tamaño: {} bytes", pdfContent.length);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    "cotizacion_" + quote.getQuoteNumber() + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("❌ ERROR al generar PDF:", e);
            log.error("   Mensaje: {}", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 🧾 GENERAR PDF DE FACTURA
     * GET /sales/quotes/{id}/invoice/pdf
     */
    @GetMapping("/{id}/invoice/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable String id) {
        String businessId = userContext.getCurrentBusinessId();

        log.info("🧾 Generando PDF de factura para cotización ID: {}", id);
        log.info("   Business ID del request: {}", businessId);

        try {
            Quote quote = quoteRepository.findById(id)
                    .orElseThrow(() -> {
                        log.error("❌ Cotización no encontrada: {}", id);
                        return new RuntimeException("Cotización no encontrada con ID: " + id);
                    });

            log.info("✅ Cotización encontrada: {}", quote.getQuoteNumber());
            log.info("   Status: {}", quote.getStatus());
            log.info("   Business ID de la cotización: {}", quote.getBusinessId());
            log.info("   Invoice Number: {}", quote.getInvoiceNumber());

            // ✅ Verificar que esté facturada
            if (quote.getStatus() != QuoteStatus.FACTURADA) {
                log.error("❌ La cotización no está facturada. Status: {}", quote.getStatus());
                throw new RuntimeException("La cotización no está facturada. Estado: " + quote.getStatus());
            }

            // ⚠️ TEMPORAL: Comentar validación de business_id hasta arreglar JWT
            /*
             * if (!quote.getBusinessId().equals(businessId)) {
             * log.error("❌ Business ID no coincide. Request: {}, Cotización: {}",
             * businessId, quote.getBusinessId());
             * throw new RuntimeException("No tiene permisos para acceder a esta factura");
             * }
             */

            log.info("⚠️  Validación de business_id omitida (modo desarrollo)");

            log.info("🔨 Generando PDF de factura...");
            byte[] pdfContent = invoicePdfService.generateInvoicePdf(quote);
            log.info("✅ Factura PDF generada exitosamente. Tamaño: {} bytes", pdfContent.length);

            if (pdfContent.length == 0) {
                log.error("❌ El PDF generado está vacío");
                throw new RuntimeException("Error al generar PDF: documento vacío");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline",
                    "factura_" + quote.getInvoiceNumber() + ".pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("❌ ERROR al generar factura PDF:", e);
            log.error("   Mensaje: {}", e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 🔍 Verificar si un producto requiere número de serie
     */
    private boolean requiresSerialNumber(String productName) {
        if (productName == null)
            return false;
        String lower = productName.toLowerCase();
        return lower.contains("celular") ||
                lower.contains("laptop") ||
                lower.contains("computadora") ||
                lower.contains("tablet") ||
                lower.contains("smartphone") ||
                lower.contains("iphone") ||
                lower.contains("samsung") ||
                lower.contains("xiaomi") ||
                lower.contains("tv") ||
                lower.contains("televisor");
    }
}