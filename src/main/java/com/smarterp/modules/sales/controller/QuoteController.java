package com.smarterp.modules.sales.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.cashier.dto.QuoteSearchResponse;
import com.smarterp.modules.inventory.entity.Stock;
import com.smarterp.modules.inventory.repository.StockRepository;
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
    private final StockRepository stockRepository;

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
        String userId = userContext.getCurrentUserId();
        int blockMinutes = (minutes != null) ? minutes : 20;

        log.info("🔒 Bloqueando cotización: {} por {} minutos - Usuario: {}", id, blockMinutes, userId);

        try {
            Quote quote = quoteService.blockQuote(id, businessId, blockMinutes);
            return ResponseEntity.ok(ApiResponse.success(
                    "Cotización bloqueada por " + blockMinutes + " minutos", quote));
        } catch (Exception e) {
            log.error("❌ Error al bloquear cotización: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<ApiResponse<Quote>> releaseBlock(@PathVariable String id) {
        String businessId = userContext.getCurrentBusinessId();
        String userId = userContext.getCurrentUserId();

        log.info("🔓 Liberando bloqueo de cotización: {} - Usuario: {}", id, userId);

        try {
            Quote quote = quoteService.releaseBlock(id, businessId);
            return ResponseEntity.ok(ApiResponse.success("Bloqueo liberado", quote));
        } catch (Exception e) {
            log.error("❌ Error al liberar bloqueo: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * ✅ CONSULTAR DISPONIBILIDAD DE PRODUCTO (STOCK - BLOQUEADO)
     */
    @GetMapping("/product/{productId}/availability")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProductAvailability(
            @PathVariable String productId) {

        String businessId = userContext.getCurrentBusinessId();

        log.info("📦 Consultando disponibilidad del producto: {}", productId);

        try {
            // Buscar cotizaciones bloqueadas que contengan este producto
            List<Quote> blockedQuotes = quoteRepository.findActiveBlockedQuotes(businessId);

            // Calcular stock bloqueado
            int blockedQuantity = blockedQuotes.stream()
                    .filter(q -> q.getItems() != null)
                    .flatMap(q -> q.getItems().stream())
                    .filter(item -> item.getProductId().equals(productId))
                    .mapToInt(QuoteItem::getQuantity)
                    .sum();

            // Obtener stock disponible
            Stock stock = stockRepository.findByProductIdAndBusinessId(productId, businessId)
                    .orElse(null);

            int totalStock = stock != null ? stock.getQuantity() : 0;
            int availableStock = Math.max(0, totalStock - blockedQuantity);

            // Obtener información de bloqueo
            List<String> blockedBy = blockedQuotes.stream()
                    .filter(q -> q.getItems() != null && q.getItems().stream()
                            .anyMatch(item -> item.getProductId().equals(productId)))
                    .map(Quote::getSellerName)
                    .distinct()
                    .collect(Collectors.toList());

            LocalDateTime blockedUntil = blockedQuotes.stream()
                    .filter(q -> q.getItems() != null && q.getItems().stream()
                            .anyMatch(item -> item.getProductId().equals(productId)))
                    .map(Quote::getBlockedUntil)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            Map<String, Object> availability = new HashMap<>();
            availability.put("productId", productId);
            availability.put("totalStock", totalStock);
            availability.put("blockedQuantity", blockedQuantity);
            availability.put("availableStock", availableStock);
            availability.put("isBlocked", blockedQuantity > 0);
            availability.put("isAvailable", availableStock > 0);
            availability.put("blockedBy", blockedBy);
            availability.put("blockedUntil", blockedUntil);

            log.info("✅ Disponibilidad: Total={}, Bloqueado={}, Disponible={}",
                    totalStock, blockedQuantity, availableStock);

            return ResponseEntity.ok(ApiResponse.success(availability));

        } catch (Exception e) {
            log.error("❌ Error al consultar disponibilidad: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Quote>>> getAllQuotes() {
        String businessId = userContext.getCurrentBusinessId();
        String userId = userContext.getCurrentUserId();

        log.info("📋 Obteniendo cotizaciones - Business: {}, Vendedor: {}", businessId, userId);

        List<Quote> quotes = quoteRepository.findByBusinessIdAndSellerIdOrderByCreatedAtDesc(
                businessId, userId);

        log.info("✅ {} cotizaciones encontradas para vendedor {}", quotes.size(), userId);

        return ResponseEntity.ok(ApiResponse.success(quotes));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<QuoteSearchResponse>>> getPendingQuotes() {
        String businessId = userContext.getCurrentBusinessId();
        String userId = userContext.getCurrentUserId();

        log.info("📋 Obteniendo cotizaciones pendientes - Business: {}, Vendedor: {}", businessId, userId);

        try {
            List<Quote> pendingQuotes = quoteRepository.findPendingQuotesByBusinessIdAndSellerId(
                    businessId, userId);

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

            log.info("✅ {} cotizaciones pendientes para vendedor {}", response.size(), userId);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("❌ Error al obtener cotizaciones pendientes: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

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
                invoices = quoteRepository.findByInvoiceNumberAndBusinessId(invoiceNumber, businessId)
                        .map(Collections::singletonList)
                        .orElse(Collections.emptyList());
            } else if (customerName != null && !customerName.isEmpty()) {
                invoices = quoteRepository.findInvoicesByCustomerName(businessId, customerName);
            } else {
                LocalDateTime start = startDate != null ? LocalDateTime.parse(startDate + "T00:00:00")
                        : LocalDateTime.now().toLocalDate().atStartOfDay();
                LocalDateTime end = endDate != null ? LocalDateTime.parse(endDate + "T23:59:59") : LocalDateTime.now();

                invoices = quoteRepository.findInvoicesByBusinessIdAndDateRange(businessId, start, end);
            }

            invoices = invoices.stream()
                    .filter(q -> q.getStatus() == QuoteStatus.FACTURADA)
                    .collect(Collectors.toList());

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
        String sellerId = userContext.getCurrentUserId(); // Email del vendedor

        log.info("📊 Obteniendo dashboard - Business: {}, Vendedor: {}", businessId, sellerId);

        try {
            Map<String, Object> dashboard = quoteService.getVendedorDashboard(businessId, sellerId);
            return ResponseEntity.ok(ApiResponse.success(dashboard));
        } catch (Exception e) {
            log.error("❌ Error al obtener dashboard: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadQuotePdf(@PathVariable String id) {
        String businessId = userContext.getCurrentBusinessId();

        try {
            Quote quote = quoteRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Cotización no encontrada"));

            if (!quote.getBusinessId().equals(businessId)) {
                throw new RuntimeException("No tiene permisos");
            }

            byte[] pdfContent = quotePdfService.generateQuotePdf(quote);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    "cotizacion_" + quote.getQuoteNumber() + ".pdf");

            return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("❌ ERROR al generar PDF:", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}/invoice/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable String id) {
        String businessId = userContext.getCurrentBusinessId();

        try {
            Quote quote = quoteRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Cotización no encontrada"));

            if (quote.getStatus() != QuoteStatus.FACTURADA) {
                throw new RuntimeException("La cotización no está facturada");
            }

            byte[] pdfContent = invoicePdfService.generateInvoicePdf(quote);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline",
                    "factura_" + quote.getInvoiceNumber() + ".pdf");

            return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("❌ ERROR al generar factura PDF:", e);
            return ResponseEntity.badRequest().build();
        }
    }

    private boolean requiresSerialNumber(String productName) {
        if (productName == null)
            return false;
        String lower = productName.toLowerCase();
        return lower.contains("celular") || lower.contains("laptop") ||
                lower.contains("computadora") || lower.contains("tablet") ||
                lower.contains("smartphone") || lower.contains("iphone") ||
                lower.contains("samsung") || lower.contains("xiaomi") ||
                lower.contains("tv") || lower.contains("televisor");
    }
}