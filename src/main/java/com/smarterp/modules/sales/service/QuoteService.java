package com.smarterp.modules.sales.service;

import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.inventory.entity.Stock;
import com.smarterp.modules.inventory.repository.StockRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final StockRepository stockRepository;
    private final UserContext userContext;

    /**
     * 📝 CREAR COTIZACIÓN
     */
    @Transactional
    public Quote createQuote(String businessId, QuoteRequest request) {
        // ✅ OBTENER EMAIL DEL VENDEDOR (ahora userId es el email)
        String sellerId = userContext.getCurrentUserId();
        String sellerName = userContext.getCurrentUserEmail();

        log.info("📝 Creando cotización - Vendedor: {} ({})", sellerName, sellerId);

        Quote quote = Quote.builder()
                .businessId(businessId)
                .sellerId(sellerId) // ✅ CRÍTICO: Guardar email como sellerId
                .sellerName(sellerName)
                .customerName(request.getCustomerName())
                .customerDocument(request.getCustomerDocument())
                .status(QuoteStatus.PENDIENTE)
                .notes(request.getNotes())
                .blockedUntil(null) // Inicialmente no bloqueado
                .isBlocked(false)
                .build();

        // Agregar items
        if (request.getItems() != null) {
            request.getItems().forEach(itemRequest -> {
                QuoteItem item = QuoteItem.builder()
                        .productId(itemRequest.getProductId())
                        .productName(itemRequest.getProductName())
                        .productSku(itemRequest.getProductSku())
                        .quantity(itemRequest.getQuantity())
                        .unitPrice(itemRequest.getUnitPrice())
                        .subtotal(itemRequest.getUnitPrice().multiply(
                                BigDecimal.valueOf(itemRequest.getQuantity())))
                        .build();
                quote.addItem(item);
            });
        }

        quote.calculateTotals();

        // ✅ ACTIVAR BLOQUEO POR 20 MINUTOS
        quote.activateBlock(20); // Bloqueo de 20 minutos

        Quote savedQuote = quoteRepository.save(quote);
        log.info("✅ Cotización creada: {} por vendedor {} - Bloqueada hasta: {}",
                savedQuote.getQuoteNumber(), sellerId, savedQuote.getBlockedUntil());

        return savedQuote;
    }

    /**
     * 🔒 BLOQUEAR COTIZACIÓN
     */
    @Transactional
    public Quote blockQuote(String quoteId, String businessId, int minutes) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("Cotización no encontrada"));

        if (!quote.getBusinessId().equals(businessId)) {
            throw new RuntimeException("No tiene permisos para esta cotización");
        }

        if (quote.getStatus() != QuoteStatus.PENDIENTE) {
            throw new RuntimeException("Solo se pueden bloquear cotizaciones pendientes");
        }

        // Activar bloqueo
        quote.activateBlock(minutes);

        Quote saved = quoteRepository.save(quote);
        log.info("✅ Cotización {} bloqueada hasta {}", saved.getQuoteNumber(), saved.getBlockedUntil());

        return saved;
    }

    /**
     * 🔓 LIBERAR BLOQUEO
     */
    @Transactional
    public Quote releaseBlock(String quoteId, String businessId) {
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("Cotización no encontrada"));

        if (!quote.getBusinessId().equals(businessId)) {
            throw new RuntimeException("No tiene permisos para esta cotización");
        }

        quote.releaseBlock();

        Quote saved = quoteRepository.save(quote);
        log.info("✅ Bloqueo de cotización {} liberado", saved.getQuoteNumber());

        return saved;
    }

    /**
     * 📦 DISPONIBILIDAD DE PRODUCTO
     */
    public Map<String, Object> getProductAvailability(String productId, String businessId) {
        // Buscar cotizaciones bloqueadas que contengan este producto
        List<Quote> blockedQuotes = quoteRepository.findActiveBlockedQuotes(businessId);

        // Filtrar por producto
        long blockedCount = blockedQuotes.stream()
                .filter(q -> q.getItems().stream()
                        .anyMatch(item -> item.getProductId().equals(productId)))
                .count();

        Map<String, Object> availability = new HashMap<>();
        availability.put("productId", productId);
        availability.put("isBlocked", blockedCount > 0);
        availability.put("blockedBy", blockedCount);
        availability.put("blockedUntil", blockedCount > 0 ? blockedQuotes.stream()
                .filter(q -> q.getItems().stream()
                        .anyMatch(item -> item.getProductId().equals(productId)))
                .map(Quote::getBlockedUntil)
                .findFirst()
                .orElse(null) : null);

        // Obtener stock disponible
        Stock stock = stockRepository.findByProductIdAndBusinessId(productId, businessId)
                .orElse(null);

        availability.put("stockAvailable", stock != null ? stock.getQuantity() : 0);

        return availability;
    }

    /**
     * 📋 OBTENER TODAS LAS COTIZACIONES
     */
    public List<Quote> getAllQuotes(String businessId) {
        return quoteRepository.findByBusinessIdOrderByCreatedAtDesc(businessId);
    }

    /**
     * 🔍 BUSCAR POR NÚMERO
     */
    public Quote getByNumber(String quoteNumber, String businessId) {
        Quote quote = quoteRepository.findByQuoteNumber(quoteNumber)
                .orElseThrow(() -> new RuntimeException("Cotización no encontrada: " + quoteNumber));

        if (!quote.getBusinessId().equals(businessId)) {
            throw new RuntimeException("Cotización no pertenece a este negocio");
        }

        return quote;
    }

        /**
     * 📊 DASHBOARD DEL VENDEDOR
     */
    public Map<String, Object> getVendedorDashboard(String businessId, String sellerId) {
        log.info("📊 Obteniendo dashboard del vendedor: {}", sellerId);
        
        Map<String, Object> dashboard = new HashMap<>();
        
        // Obtener todas las cotizaciones del vendedor
        List<Quote> allQuotes = quoteRepository.findByBusinessIdAndSellerIdOrderByCreatedAtDesc(businessId, sellerId);
        
        // Filtrar por fecha
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        
        // Cotizaciones de hoy
        List<Quote> todayQuotes = allQuotes.stream()
                .filter(q -> q.getCreatedAt().isAfter(todayStart))
                .collect(Collectors.toList());
        
        // Cotizaciones de los últimos 7 días
        List<Quote> last7DaysQuotes = allQuotes.stream()
                .filter(q -> q.getCreatedAt().isAfter(sevenDaysAgo))
                .collect(Collectors.toList());
        
        // Calcular totales
        long totalCotizaciones = allQuotes.size();
        long cotizacionesHoy = todayQuotes.size();
        long cotizacionesPendientes = allQuotes.stream()
                .filter(q -> q.getStatus() == QuoteStatus.PENDIENTE)
                .count();
        long cotizacionesFacturadas = allQuotes.stream()
                .filter(q -> q.getStatus() == QuoteStatus.FACTURADA)
                .count();
        
        // Calcular montos
        BigDecimal ventasHoy = todayQuotes.stream()
                .filter(q -> q.getStatus() == QuoteStatus.FACTURADA)
                .map(Quote::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal ventas7Dias = last7DaysQuotes.stream()
                .filter(q -> q.getStatus() == QuoteStatus.FACTURADA)
                .map(Quote::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalVendido = allQuotes.stream()
                .filter(q -> q.getStatus() == QuoteStatus.FACTURADA)
                .map(Quote::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Calcular tasa de conversión
        double tasaConversion = totalCotizaciones > 0 
                ? (cotizacionesFacturadas * 100.0 / totalCotizaciones) 
                : 0.0;
        
        dashboard.put("totalCotizaciones", totalCotizaciones);
        dashboard.put("cotizacionesHoy", cotizacionesHoy);
        dashboard.put("cotizacionesPendientes", cotizacionesPendientes);
        dashboard.put("cotizacionesFacturadas", cotizacionesFacturadas);
        dashboard.put("ventasHoy", ventasHoy);
        dashboard.put("ventas7Dias", ventas7Dias);
        dashboard.put("totalVendido", totalVendido);
        dashboard.put("tasaConversion", tasaConversion);
        
        log.info("✅ Dashboard calculado: {} cotizaciones, S/ {} ventas hoy", 
                cotizacionesHoy, ventasHoy);
        
        return dashboard;
    }
}