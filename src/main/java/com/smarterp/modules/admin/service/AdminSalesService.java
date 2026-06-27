package com.smarterp.modules.admin.service;

import com.smarterp.modules.sales.entity.Quote;
import com.smarterp.modules.sales.entity.QuoteStatus;
import com.smarterp.modules.sales.repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminSalesService {

    private final QuoteRepository quoteRepository;

    /**
     * 📊 DASHBOARD DE VENTAS - ADMIN (DATOS REALES)
     */
    public Map<String, Object> getSalesDashboard(String businessId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("📊 [ADMIN] Dashboard - Business: {} - Desde: {} - Hasta: {}", businessId, startDate, endDate);

        Map<String, Object> dashboard = new HashMap<>();

        List<Quote> allQuotes = quoteRepository.findByBusinessIdOrderByCreatedAtDesc(businessId);

        log.info("📊 [ADMIN] Total de cotizaciones en el sistema: {}", allQuotes.size());

        List<Quote> periodQuotes = allQuotes.stream()
                .filter(q -> q.getCreatedAt() != null &&
                        !q.getCreatedAt().isBefore(startDate) &&
                        !q.getCreatedAt().isAfter(endDate))
                .collect(Collectors.toList());

        List<Quote> pendingQuotes = periodQuotes.stream()
                .filter(q -> q.getStatus() == QuoteStatus.PENDIENTE)
                .collect(Collectors.toList());

        List<Quote> completedQuotes = periodQuotes.stream()
                .filter(q -> q.getStatus() == QuoteStatus.FACTURADA || q.getStatus() == QuoteStatus.PAGADA)
                .collect(Collectors.toList());

        BigDecimal totalVentas = completedQuotes.stream()
                .map(Quote::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPendiente = pendingQuotes.stream()
                .map(Quote::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime todayEnd = LocalDateTime.now();

        List<Quote> todayQuotes = allQuotes.stream()
                .filter(q -> q.getCreatedAt() != null &&
                        !q.getCreatedAt().isBefore(todayStart) &&
                        !q.getCreatedAt().isAfter(todayEnd))
                .collect(Collectors.toList());

        BigDecimal ventasHoy = todayQuotes.stream()
                .filter(q -> q.getStatus() == QuoteStatus.FACTURADA || q.getStatus() == QuoteStatus.PAGADA)
                .map(Quote::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long cotizacionesHoy = todayQuotes.size();

        double tasaConversion = periodQuotes.size() > 0
                ? (completedQuotes.size() * 100.0 / periodQuotes.size())
                : 0.0;

        Map<String, BigDecimal> ventasPorMetodo = completedQuotes.stream()
                .filter(q -> q.getPaymentMethod() != null)
                .collect(Collectors.groupingBy(
                        Quote::getPaymentMethod,
                        Collectors.reducing(BigDecimal.ZERO, Quote::getTotal, BigDecimal::add)));

        dashboard.put("totalVentas", totalVentas);
        dashboard.put("totalPendiente", totalPendiente);
        dashboard.put("ventasHoy", ventasHoy);
        dashboard.put("cotizacionesHoy", cotizacionesHoy);
        dashboard.put("totalCotizaciones", periodQuotes.size());
        dashboard.put("cotizacionesPendientes", pendingQuotes.size());
        dashboard.put("cotizacionesCompletadas", completedQuotes.size());
        dashboard.put("tasaConversion", tasaConversion);
        dashboard.put("ventasPorMetodo", ventasPorMetodo);
        dashboard.put("periodo", Map.of(
                "inicio", startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                "fin", endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)));

        log.info("✅ [ADMIN] Dashboard: {} cotizaciones, S/ {} ventas, {} hoy, S/ {} hoy",
                periodQuotes.size(), totalVentas, cotizacionesHoy, ventasHoy);

        return dashboard;
    }

    /**
     * 📋 TODAS LAS COTIZACIONES
     */
    public Map<String, Object> getAllQuotes(String businessId, int page, int size,
            String status, String sellerId, String startDate, String endDate) {

        log.info("📋 [ADMIN] Obteniendo cotizaciones - Business: {} - Page: {} - Size: {}", businessId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Quote> quotesPage;

        try {
            if (status != null && !status.isEmpty()) {
                QuoteStatus quoteStatus = QuoteStatus.valueOf(status.toUpperCase());
                if (sellerId != null && !sellerId.isEmpty()) {
                    quotesPage = quoteRepository.findByBusinessIdAndSellerIdAndStatus(
                            businessId, sellerId, quoteStatus, pageable);
                } else {
                    quotesPage = quoteRepository.findByBusinessIdAndStatus(
                            businessId, quoteStatus, pageable);
                }
            } else if (sellerId != null && !sellerId.isEmpty()) {
                quotesPage = quoteRepository.findByBusinessIdAndSellerId(
                        businessId, sellerId, pageable);
            } else {
                quotesPage = quoteRepository.findByBusinessId(businessId, pageable);
            }

            // ✅ Simplificado: Solo usar sellerId directamente
            List<Map<String, Object>> quotesWithSeller = new ArrayList<>();

            for (Quote quote : quotesPage.getContent()) {
                Map<String, Object> quoteMap = new HashMap<>();

                quoteMap.put("id", quote.getId());
                quoteMap.put("quoteNumber", quote.getQuoteNumber());
                quoteMap.put("customerName", quote.getCustomerName());
                quoteMap.put("customerDocument", quote.getCustomerDocument());
                quoteMap.put("sellerId", quote.getSellerId());
                quoteMap.put("sellerName", quote.getSellerName());
                quoteMap.put("sellerEmail", quote.getSellerId()); // ✅ Usar sellerId como email

                quoteMap.put("status", quote.getStatus());
                quoteMap.put("total", quote.getTotal());
                quoteMap.put("subtotal", quote.getSubtotal());
                quoteMap.put("igv", quote.getIgv());
                quoteMap.put("paymentMethod", quote.getPaymentMethod());
                quoteMap.put("createdAt", quote.getCreatedAt());
                quoteMap.put("paidAt", quote.getPaidAt());
                quoteMap.put("items", quote.getItems());

                quotesWithSeller.add(quoteMap);
            }

            BigDecimal totalVentas = quotesPage.getContent().stream()
                    .filter(q -> q.getStatus() == QuoteStatus.FACTURADA || q.getStatus() == QuoteStatus.PAGADA)
                    .map(Quote::getTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> response = new HashMap<>();
            response.put("content", quotesWithSeller);
            response.put("totalElements", quotesPage.getTotalElements());
            response.put("totalPages", quotesPage.getTotalPages());
            response.put("currentPage", quotesPage.getNumber());
            response.put("size", quotesPage.getSize());
            response.put("totalVentas", totalVentas);

            return response;

        } catch (Exception e) {
            log.error("❌ [ADMIN] Error al obtener cotizaciones: {}", e.getMessage());
            return Map.of(
                    "content", List.of(),
                    "totalElements", 0,
                    "totalPages", 0,
                    "currentPage", 0,
                    "size", size,
                    "totalVentas", BigDecimal.ZERO);
        }
    }

    /**
     * 💰 VENTAS POR PERÍODO
     */
    public Map<String, Object> getSalesByPeriod(String businessId, String period,
            LocalDateTime startDate, LocalDateTime endDate) {

        log.info("💰 [ADMIN] Ventas por período: {} - Business: {}", period, businessId);

        List<Quote> allQuotes = quoteRepository.findByBusinessIdOrderByCreatedAtDesc(businessId);

        List<Quote> completedQuotes = allQuotes.stream()
                .filter(q -> (q.getStatus() == QuoteStatus.FACTURADA || q.getStatus() == QuoteStatus.PAGADA))
                .filter(q -> q.getPaidAt() != null)
                .filter(q -> !q.getPaidAt().isBefore(startDate) && !q.getPaidAt().isAfter(endDate))
                .collect(Collectors.toList());

        Map<String, BigDecimal> salesByPeriod = new LinkedHashMap<>();

        try {
            if ("day".equals(period)) {
                salesByPeriod = completedQuotes.stream()
                        .collect(Collectors.groupingBy(
                                q -> q.getPaidAt().toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                                LinkedHashMap::new,
                                Collectors.reducing(BigDecimal.ZERO, Quote::getTotal, BigDecimal::add)));
            } else if ("week".equals(period)) {
                salesByPeriod = completedQuotes.stream()
                        .collect(Collectors.groupingBy(
                                q -> {
                                    int week = q.getPaidAt().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                                    int year = q.getPaidAt().get(IsoFields.WEEK_BASED_YEAR);
                                    return String.format("%d-S%02d", year, week);
                                },
                                LinkedHashMap::new,
                                Collectors.reducing(BigDecimal.ZERO, Quote::getTotal, BigDecimal::add)));
            } else if ("month".equals(period)) {
                salesByPeriod = completedQuotes.stream()
                        .collect(Collectors.groupingBy(
                                q -> q.getPaidAt().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                                LinkedHashMap::new,
                                Collectors.reducing(BigDecimal.ZERO, Quote::getTotal, BigDecimal::add)));
            }
        } catch (Exception e) {
            log.error("❌ [ADMIN] Error al agrupar por período: {}", e.getMessage());
        }

        BigDecimal totalVentas = completedQuotes.stream()
                .map(Quote::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> response = new HashMap<>();
        response.put("salesByPeriod", salesByPeriod);
        response.put("totalVentas", totalVentas);
        response.put("periodo", Map.of(
                "tipo", period,
                "inicio", startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                "fin", endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)));

        log.info("💰 [ADMIN] Ventas por {}: {} períodos, Total: S/ {}", period, salesByPeriod.size(), totalVentas);

        return response;
    }
}