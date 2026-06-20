package com.smarterp.modules.sales.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.sales.dto.QuoteRequest;
import com.smarterp.modules.sales.entity.Quote;
import com.smarterp.modules.sales.repository.QuoteRepository;
import com.smarterp.modules.sales.service.QuotePdfService;
import com.smarterp.modules.sales.service.QuoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired; // ✅ AGREGAR
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sales/quotes")
@RequiredArgsConstructor
@Slf4j
public class QuoteController {

    private final QuoteService quoteService;
    private final QuotePdfService quotePdfService;
    private final UserContext userContext;

    // ✅ AGREGAR con @Autowired (no usar final)
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

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<Quote>>> getPendingQuotes() {
        String businessId = userContext.getCurrentBusinessId();
        return ResponseEntity.ok(ApiResponse.success(
                quoteService.getPendingQuotes(businessId)));
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

    // ✅ NUEVO ENDPOINT: Descargar PDF
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadQuotePdf(@PathVariable String id) {
        String businessId = userContext.getCurrentBusinessId();

        log.info("📄 Generando PDF para cotización ID: {}", id);
        log.info("   Business ID: {}", businessId);

        try {
            // Buscar por ID (UUID)
            Quote quote = quoteRepository.findById(id)
                    .orElseThrow(() -> {
                        log.error("❌ Cotización no encontrada: {}", id);
                        return new RuntimeException("Cotización no encontrada con ID: " + id);
                    });

            log.info("✅ Cotización encontrada: {}", quote.getQuoteNumber());
            log.info("   Cliente: {}", quote.getCustomerName());
            log.info("   Total: {}", quote.getTotal());
            log.info("   Items: {}", quote.getItems().size());

            // Verificar permisos
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
}