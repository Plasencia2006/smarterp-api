package com.smarterp.modules.sales.service;

import com.smarterp.modules.inventory.entity.Stock;
import com.smarterp.modules.inventory.repository.StockRepository;
import com.smarterp.modules.sales.dto.QuoteItemRequest;
import com.smarterp.modules.sales.dto.QuoteRequest;
import com.smarterp.modules.sales.entity.Quote;
import com.smarterp.modules.sales.entity.QuoteItem;
import com.smarterp.modules.sales.entity.QuoteStatus;
import com.smarterp.modules.sales.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final StockRepository stockRepository;

    @Transactional
    public Quote createQuote(String businessId, QuoteRequest request) {
        log.info("📝 Creando cotización - Cliente: {}", request.getCustomerName());

        if (request.getCustomerName() == null || request.getCustomerName().trim().isEmpty()) {
            throw new RuntimeException("El nombre del cliente es obligatorio");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Debe agregar al menos un producto");
        }

        // Validar stock disponible (considerando bloqueos)
        for (QuoteItemRequest itemReq : request.getItems()) {
            log.info("   Validando stock: {} (cantidad: {})",
                    itemReq.getProductName(), itemReq.getQuantity());

            if (itemReq.getProductId() == null) {
                throw new RuntimeException("Producto sin ID: " + itemReq.getProductName());
            }

            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new RuntimeException("Cantidad inválida para: " + itemReq.getProductName());
            }

            if (itemReq.getUnitPrice() == null) {
                throw new RuntimeException("Precio inválido para: " + itemReq.getProductName());
            }

            Stock stock = stockRepository.findByProductIdAndBusinessId(
                    itemReq.getProductId(), businessId)
                    .orElseThrow(() -> new RuntimeException(
                            "Producto no encontrado en stock: " + itemReq.getProductName()));

            // Calcular stock disponible (total - bloqueado)
            int blockedQuantity = getBlockedQuantityForProduct(itemReq.getProductId(), businessId);
            int availableStock = stock.getQuantity() - blockedQuantity;

            if (availableStock < itemReq.getQuantity()) {
                throw new RuntimeException(
                        "Stock insuficiente para: " + itemReq.getProductName() +
                                ". Disponible: " + availableStock +
                                " (Total: " + stock.getQuantity() +
                                ", Bloqueado: " + blockedQuantity +
                                "), Solicitado: " + itemReq.getQuantity());
            }
        }

        // Crear cotización
        Quote quote = Quote.builder()
                .businessId(businessId)
                .sellerId(null)
                .customerName(request.getCustomerName().trim())
                .customerDocument(request.getCustomerDocument() != null ? request.getCustomerDocument().trim() : null)
                .sellerName(request.getSellerName() != null ? request.getSellerName() : "Vendedor POS")
                .notes(request.getNotes() != null ? request.getNotes() : "")
                .status(QuoteStatus.PENDIENTE)
                .build();

        // Agregar items
        for (QuoteItemRequest itemReq : request.getItems()) {
            BigDecimal unitPrice = itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : BigDecimal.ZERO;
            Integer quantity = itemReq.getQuantity() != null ? itemReq.getQuantity() : 1;

            QuoteItem item = QuoteItem.builder()
                    .productId(itemReq.getProductId())
                    .productName(itemReq.getProductName())
                    .productSku(itemReq.getProductSku() != null ? itemReq.getProductSku() : "")
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .build();

            quote.addItem(item);
            log.info("    Item agregado: {} x {} = S/ {}",
                    itemReq.getProductName(), quantity,
                    unitPrice.multiply(BigDecimal.valueOf(quantity)));
        }

        quote.calculateTotals();

        //  BLOQUEO AUTOMÁTICO POR 20 MINUTOS
        quote.activateBlock(20);
        log.info("🔒 Productos bloqueados automáticamente por 20 minutos");
        log.info("   Total productos bloqueados: {}", quote.getItems().size());

        for (QuoteItem item : quote.getItems()) {
            log.info("   - {} ({} unidades)", item.getProductName(), item.getQuantity());
        }

        Quote saved = quoteRepository.save(quote);

        log.info(" Cotización creada y bloqueada: {}", saved.getQuoteNumber());
        log.info("   Total: S/ {}", saved.getTotal());
        log.info("   Cliente: {}", saved.getCustomerName());
        log.info("   Bloqueado hasta: {}", saved.getBlockedUntil());

        return saved;
    }

    @Transactional
    public Quote blockQuote(String quoteId, String businessId, Integer minutes) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("Cotización no encontrada"));

        if (!quote.getBusinessId().equals(businessId)) {
            throw new RuntimeException("No tiene permisos");
        }

        if (quote.getStatus() != QuoteStatus.PENDIENTE) {
            throw new RuntimeException("Solo se pueden bloquear cotizaciones pendientes");
        }

        int blockMinutes = (minutes != null && minutes > 0) ? minutes : 20;
        quote.activateBlock(blockMinutes);

        Quote updated = quoteRepository.save(quote);

        log.info("🔒 Cotización bloqueada por {} minutos: {}", blockMinutes, quote.getQuoteNumber());

        return updated;
    }

    @Transactional
    public Quote releaseBlock(String quoteId, String businessId) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("Cotización no encontrada"));

        if (!quote.getBusinessId().equals(businessId)) {
            throw new RuntimeException("No tiene permisos");
        }

        quote.releaseBlock();

        Quote updated = quoteRepository.save(quote);
        log.info("🔓 Bloqueo liberado: {}", quote.getQuoteNumber());

        return updated;
    }

    public int getBlockedQuantityForProduct(String productId, String businessId) {
        List<Quote> activeQuotes = quoteRepository.findByBusinessIdAndStatus(businessId, QuoteStatus.PENDIENTE);

        int totalBlocked = 0;

        for (Quote quote : activeQuotes) {
            if (quote.getIsBlocked() != null && quote.getIsBlocked() && !quote.isExpired()) {
                for (QuoteItem item : quote.getItems()) {
                    if (item.getProductId().equals(productId)) {
                        totalBlocked += item.getQuantity();
                    }
                }
            }
        }

        return totalBlocked;
    }

    public Map<String, Object> getProductAvailability(String productId, String businessId) {
        Stock stock = stockRepository.findByProductIdAndBusinessId(productId, businessId)
                .orElse(null);

        if (stock == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("productId", productId);
            response.put("totalStock", 0);
            response.put("blockedQuantity", 0);
            response.put("availableStock", 0);
            response.put("isAvailable", false);
            return response;
        }

        int blockedQuantity = getBlockedQuantityForProduct(productId, businessId);
        int availableStock = stock.getQuantity() - blockedQuantity;

        Map<String, Object> response = new HashMap<>();
        response.put("productId", productId);
        response.put("productName", stock.getProductName());
        response.put("totalStock", stock.getQuantity());
        response.put("blockedQuantity", blockedQuantity);
        response.put("availableStock", availableStock);
        response.put("isAvailable", availableStock > 0);

        return response;
    }

    public List<Quote> getPendingQuotes(String businessId) {
        return quoteRepository.findByBusinessIdAndStatus(businessId, QuoteStatus.PENDIENTE);
    }

    public Quote getByNumber(String quoteNumber, String businessId) {
        Quote quote = quoteRepository.findByQuoteNumber(quoteNumber)
                .orElseThrow(() -> new RuntimeException("Cotización no encontrada"));

        if (!quote.getBusinessId().equals(businessId)) {
            throw new RuntimeException("No tiene permisos");
        }

        return quote;
    }

    public List<Quote> getAllQuotes(String businessId) {
        return quoteRepository.findByBusinessIdOrderByCreatedAtDesc(businessId);
    }

    /**
     * 📊 Dashboard del Vendedor
     */
    public Map<String, Object> getVendedorDashboard(String businessId) {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime yesterday = today.minusDays(1);
        LocalDateTime last7Days = today.minusDays(7);
        LocalDateTime last30Days = today.minusDays(30);

        List<Quote> allQuotes = quoteRepository.findByBusinessIdOrderByCreatedAtDesc(businessId);

        // Cotizaciones de hoy
        long quotesToday = allQuotes.stream()
                .filter(q -> q.getCreatedAt().isAfter(today))
                .count();

        // Cotizaciones pendientes
        long pendingQuotes = allQuotes.stream()
                .filter(q -> q.getStatus() == QuoteStatus.PENDIENTE)
                .count();

        // Cotizaciones pagadas
        long paidQuotes = allQuotes.stream()
                .filter(q -> q.getStatus() == QuoteStatus.PAGADA)
                .count();

        // Total vendido (pagadas)
        double totalSales = allQuotes.stream()
                .filter(q -> q.getStatus() == QuoteStatus.PAGADA)
                .mapToDouble(q -> q.getTotal().doubleValue())
                .sum();

        // Ventas de hoy
        double salesToday = allQuotes.stream()
                .filter(q -> q.getStatus() == QuoteStatus.PAGADA && q.getCreatedAt().isAfter(today))
                .mapToDouble(q -> q.getTotal().doubleValue())
                .sum();

        // Ventas últimos 7 días
        double salesLast7Days = allQuotes.stream()
                .filter(q -> q.getStatus() == QuoteStatus.PAGADA && q.getCreatedAt().isAfter(last7Days))
                .mapToDouble(q -> q.getTotal().doubleValue())
                .sum();

        // Tasa de conversión
        double conversionRate = allQuotes.size() > 0 ? (paidQuotes * 100.0) / allQuotes.size() : 0;

        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("quotesToday", quotesToday);
        dashboard.put("pendingQuotes", pendingQuotes);
        dashboard.put("paidQuotes", paidQuotes);
        dashboard.put("totalSales", totalSales);
        dashboard.put("salesToday", salesToday);
        dashboard.put("salesLast7Days", salesLast7Days);
        dashboard.put("conversionRate", conversionRate);
        dashboard.put("totalQuotes", allQuotes.size());

        return dashboard;
    }
}