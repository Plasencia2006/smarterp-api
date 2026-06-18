package com.smarterp.modules.inventory.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.inventory.entity.*;
import com.smarterp.modules.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/inventory/dashboard")
@RequiredArgsConstructor
@Slf4j
public class InventoryDashboardController {

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    private final StockAlertRepository alertRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ProductCategoryRepository categoryRepository;
    private final UserContext userContext;

    /**
     * Obtener estadísticas completas del dashboard
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        String businessId = userContext.getCurrentBusinessId();
        log.info("📊 Obteniendo estadísticas del dashboard para business: {}", businessId);

        Map<String, Object> stats = new HashMap<>();

        // 1️⃣ Total de productos
        long totalProducts = productRepository.findByBusinessId(businessId).size();
        stats.put("totalProducts", totalProducts);

        // 2️⃣ Stock bajo
        List<Stock> lowStock = stockRepository.findLowStock(businessId);
        stats.put("lowStock", lowStock.size());

        // 3️⃣ Entradas hoy
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long entriesToday = movementRepository.findByBusinessId(businessId).stream()
                .filter(m -> m.getCreatedAt() != null && m.getCreatedAt().isAfter(startOfDay))
                .filter(m -> m.getType() == StockMovementType.IN)
                .count();
        stats.put("entriesToday", entriesToday);

        // 4️⃣ Valor del inventario
        BigDecimal inventoryValue = stockRepository.findByBusinessId(businessId).stream()
                .map(stock -> {
                    // Buscar producto para obtener precio
                    return productRepository.findById(stock.getProductId())
                            .map(product -> product.getPrice().multiply(BigDecimal.valueOf(stock.getQuantity())))
                            .orElse(BigDecimal.ZERO);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("inventoryValue", inventoryValue);

        // 5️⃣ Alertas pendientes
        long pendingAlerts = alertRepository.findByBusinessIdAndIsAttended(businessId, false).size();
        stats.put("pendingAlerts", pendingAlerts);

        // 6️⃣ Órdenes de compra pendientes
        long pendingOrders = purchaseOrderRepository.findByBusinessIdOrderByCreatedAtDesc(businessId).stream()
                .filter(order -> order.getStatus() == PurchaseStatus.PENDING)
                .count();
        stats.put("pendingOrders", pendingOrders);

        log.info("✅ Estadísticas obtenidas: {}", stats);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Obtener datos para gráfico de movimientos (últimos 7 días)
     */
    @GetMapping("/movements-chart")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMovementsChart() {
        String businessId = userContext.getCurrentBusinessId();

        List<Map<String, Object>> chartData = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Últimos 7 días
        for (int i = 6; i >= 0; i--) {
            LocalDateTime dayStart = now.minusDays(i).with(LocalTime.MIN);
            LocalDateTime dayEnd = now.minusDays(i).with(LocalTime.MAX);

            List<StockMovement> movements = movementRepository.findByBusinessId(businessId).stream()
                    .filter(m -> m.getCreatedAt() != null &&
                            !m.getCreatedAt().isBefore(dayStart) &&
                            !m.getCreatedAt().isAfter(dayEnd))
                    .collect(Collectors.toList());

            long entries = movements.stream()
                    .filter(m -> m.getType() == StockMovementType.IN)
                    .mapToLong(StockMovement::getQuantity)
                    .sum();

            long exits = movements.stream()
                    .filter(m -> m.getType() == StockMovementType.OUT)
                    .mapToLong(StockMovement::getQuantity)
                    .sum();

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("day", dayStart.toLocalDate().getDayOfWeek().toString().substring(0, 3));
            dayData.put("entries", entries);
            dayData.put("exits", exits);
            chartData.add(dayData);
        }

        return ResponseEntity.ok(ApiResponse.success(chartData));
    }

    /**
     * Obtener distribución por categorías
     */
    @GetMapping("/categories-distribution")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCategoriesDistribution() {
        String businessId = userContext.getCurrentBusinessId();

        List<ProductCategory> categories = categoryRepository.findByBusinessId(businessId);
        List<Product> products = productRepository.findByBusinessId(businessId);

        List<Map<String, Object>> distribution = new ArrayList<>();

        for (ProductCategory category : categories) {
            long count = products.stream()
                    .filter(p -> category.getId().equals(p.getCategoryId()))
                    .count();

            if (count > 0) {
                Map<String, Object> catData = new HashMap<>();
                catData.put("name", category.getName());
                catData.put("value", count);
                catData.put("color", category.getColor() != null ? category.getColor() : "#3B82F6");
                distribution.add(catData);
            }
        }

        // Agregar "Sin categoría" si hay productos sin categoría
        long uncategorized = products.stream()
                .filter(p -> p.getCategoryId() == null || p.getCategoryId().isEmpty())
                .count();

        if (uncategorized > 0) {
            Map<String, Object> uncategorizedData = new HashMap<>();
            uncategorizedData.put("name", "Sin categoría");
            uncategorizedData.put("value", uncategorized);
            uncategorizedData.put("color", "#6B7280");
            distribution.add(uncategorizedData);
        }

        return ResponseEntity.ok(ApiResponse.success(distribution));
    }

    /**
     * Obtener productos con stock más bajo
     */
    @GetMapping("/low-stock-products")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLowStockProducts() {
        String businessId = userContext.getCurrentBusinessId();

        List<Stock> stocks = stockRepository.findLowStock(businessId);

        List<Map<String, Object>> lowStockProducts = stocks.stream()
                .sorted(Comparator.comparingInt(Stock::getQuantity))
                .limit(10)
                .map(stock -> {
                    Map<String, Object> productData = new HashMap<>();
                    productData.put("id", stock.getProductId());
                    productData.put("name", stock.getProductName());
                    productData.put("sku", stock.getProductSku());
                    productData.put("quantity", stock.getQuantity());
                    productData.put("minStock", stock.getMinStock());
                    productData.put("status", stock.getQuantity() <= 0 ? "OUT_OF_STOCK" : "LOW_STOCK");
                    return productData;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(lowStockProducts));
    }

    /**
     * Obtener movimientos recientes
     */
    @GetMapping("/recent-movements")
    public ResponseEntity<ApiResponse<List<StockMovement>>> getRecentMovements() {
        String businessId = userContext.getCurrentBusinessId();

        List<StockMovement> movements = movementRepository.findTop10ByBusinessIdOrderByCreatedAtDesc(businessId);

        return ResponseEntity.ok(ApiResponse.success(movements));
    }
}