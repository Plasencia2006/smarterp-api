package com.smarterp.modules.admin.service;

import com.smarterp.modules.sales.entity.Quote;
import com.smarterp.modules.sales.entity.QuoteStatus;
import com.smarterp.modules.sales.repository.QuoteRepository;
import com.smarterp.modules.inventory.entity.Product;
import com.smarterp.modules.inventory.entity.Stock;
import com.smarterp.modules.inventory.entity.ProductCategory;
import com.smarterp.modules.inventory.repository.ProductRepository;
import com.smarterp.modules.inventory.repository.StockRepository;
import com.smarterp.modules.inventory.repository.ProductCategoryRepository;
import com.smarterp.modules.cashier.entity.CashRegister;
import com.smarterp.modules.cashier.entity.CashRegisterStatus;
import com.smarterp.modules.cashier.repository.CashRegisterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardService {

    private final QuoteRepository quoteRepository;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final CashRegisterRepository registerRepository;
    private final ProductCategoryRepository categoryRepository;

    /**
     * 📊 OBTENER ESTADÍSTICAS GENERALES DEL DASHBOARD
     */
    public Map<String, Object> getDashboardStats(String businessId) {
        log.info("📊 Obteniendo estadísticas del dashboard - Business: {}", businessId);

        Map<String, Object> stats = new HashMap<>();

        // Obtener datos
        List<Quote> allQuotes = quoteRepository.findByBusinessIdOrderByCreatedAtDesc(businessId);
        List<Product> allProducts = productRepository.findByBusinessId(businessId);
        List<Stock> allStock = stockRepository.findByBusinessId(businessId);
        List<CashRegister> allRegisters = registerRepository.findByBusinessIdOrderByOpeningTimeDesc(businessId);

        // 👥 Total de productos (en lugar de miembros)
        stats.put("totalMembers", allProducts.size());
        stats.put("totalProducts", allProducts.size());

        // 💰 Ventas del mes
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        List<Quote> monthlyQuotes = allQuotes.stream()
                .filter(q -> q.getCreatedAt() != null && !q.getCreatedAt().isBefore(startOfMonth))
                .collect(Collectors.toList());

        BigDecimal monthlySales = monthlyQuotes.stream()
                .filter(q -> q.getStatus() == QuoteStatus.PAGADA || q.getStatus() == QuoteStatus.FACTURADA)
                .map(q -> q.getTotal() != null ? q.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("monthlySales", monthlySales);

        // 📈 Ventas del día
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        BigDecimal dailySales = allQuotes.stream()
                .filter(q -> q.getCreatedAt() != null && !q.getCreatedAt().isBefore(startOfDay))
                .filter(q -> q.getStatus() == QuoteStatus.PAGADA || q.getStatus() == QuoteStatus.FACTURADA)
                .map(q -> q.getTotal() != null ? q.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("dailySales", dailySales);

        // 📊 Conteo de cotizaciones por estado
        Map<String, Long> quotesByStatus = allQuotes.stream()
                .filter(q -> q.getStatus() != null)
                .collect(Collectors.groupingBy(q -> q.getStatus().toString(), Collectors.counting()));
        stats.put("quotesByStatus", quotesByStatus);

        // 🏪 Estado del negocio
        stats.put("businessStatus", "Activo");

        // 📉 Productos con stock bajo
        Map<String, Stock> stockMap = new HashMap<>();
        for (Stock stock : allStock) {
            stockMap.put(stock.getProductId(), stock);
        }

        long lowStockCount = allProducts.stream()
                .filter(p -> {
                    Stock stock = stockMap.get(p.getId());
                    int qty = stock != null ? stock.getQuantity() : 0;
                    int min = p.getMinStock() != null ? p.getMinStock() : 5;
                    return qty < min;
                })
                .count();
        stats.put("lowStockProducts", lowStockCount);
        stats.put("pendingAlerts", lowStockCount);

        // 💵 Cajas abiertas
        long openRegisters = allRegisters.stream()
                .filter(r -> r.getStatus() == CashRegisterStatus.ABIERTO)
                .count();
        stats.put("openRegisters", openRegisters);

        // 📈 Ventas de los últimos 7 días
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Quote> lastWeekQuotes = allQuotes.stream()
                .filter(q -> q.getCreatedAt() != null && !q.getCreatedAt().isBefore(sevenDaysAgo))
                .collect(Collectors.toList());

        Map<String, Double> salesLast7Days = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            double daySales = lastWeekQuotes.stream()
                    .filter(q -> q.getCreatedAt().toLocalDate().equals(date))
                    .filter(q -> q.getStatus() == QuoteStatus.PAGADA || q.getStatus() == QuoteStatus.FACTURADA)
                    .mapToDouble(q -> q.getTotal() != null ? q.getTotal().doubleValue() : 0)
                    .sum();
            salesLast7Days.put(dateStr, daySales);
        }
        stats.put("salesLast7Days", salesLast7Days);

        // 🏆 Top 5 productos (basado en cotizaciones)
        Map<String, Long> topProducts = new LinkedHashMap<>();
        // Simplificado: contar menciones de productos en cotizaciones
        stats.put("topProducts", topProducts);

        // 📊 Distribución por categoría de inventario
        Map<String, Long> productsByCategory = new LinkedHashMap<>();
        Map<String, String> categoryNameCache = new HashMap<>();

        for (Product product : allProducts) {
            String categoryId = product.getCategoryId();
            if (categoryId != null && !categoryId.isEmpty()) {
                String categoryName = categoryNameCache.computeIfAbsent(categoryId, id -> {
                    try {
                        return categoryRepository.findById(id)
                                .map(ProductCategory::getName)
                                .orElse("Sin categoría");
                    } catch (Exception e) {
                        return "Sin categoría";
                    }
                });
                productsByCategory.merge(categoryName, 1L, Long::sum);
            } else {
                productsByCategory.merge("Sin categoría", 1L, Long::sum);
            }
        }
        stats.put("productsByCategory", productsByCategory);

        // 💰 Valor total del inventario
        Map<String, Product> productMap = new HashMap<>();
        for (Product p : allProducts) {
            productMap.put(p.getId(), p);
        }

        BigDecimal inventoryValue = allStock.stream()
                .map(stock -> {
                    Product product = productMap.get(stock.getProductId());
                    if (product != null && product.getPrice() != null) {
                        return product.getPrice().multiply(BigDecimal.valueOf(stock.getQuantity()));
                    }
                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("inventoryValue", inventoryValue);

        // 📊 Total de cotizaciones
        stats.put("totalQuotes", allQuotes.size());

        // 📊 Cotizaciones pendientes
        long pendingQuotes = allQuotes.stream()
                .filter(q -> q.getStatus() == QuoteStatus.PENDIENTE)
                .count();
        stats.put("pendingQuotes", pendingQuotes);

        log.info("✅ Estadísticas del dashboard calculadas correctamente");
        return stats;
    }

    /**
     * 📈 ACTIVIDAD RECIENTE
     */
    public List<Map<String, Object>> getRecentActivity(String businessId) {
        List<Map<String, Object>> activities = new ArrayList<>();

        List<Quote> recentQuotes = quoteRepository.findByBusinessIdOrderByCreatedAtDesc(businessId)
                .stream().limit(10).collect(Collectors.toList());

        for (Quote quote : recentQuotes) {
            Map<String, Object> activity = new HashMap<>();
            activity.put("type", "SALE");
            activity.put("description",
                    "Cotización: " + (quote.getQuoteNumber() != null ? quote.getQuoteNumber() : "N/A"));
            activity.put("customer", quote.getCustomerName() != null ? quote.getCustomerName() : "Cliente");
            activity.put("amount", quote.getTotal() != null ? quote.getTotal() : BigDecimal.ZERO);
            activity.put("status", quote.getStatus() != null ? quote.getStatus().toString() : "N/A");
            activity.put("date", quote.getCreatedAt());
            activity.put("icon", "shopping-cart");
            activities.add(activity);
        }

        return activities;
    }
}