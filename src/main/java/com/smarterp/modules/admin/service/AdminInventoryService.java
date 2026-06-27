package com.smarterp.modules.admin.service;

import com.smarterp.modules.inventory.entity.Product;
import com.smarterp.modules.inventory.entity.ProductCategory;
import com.smarterp.modules.inventory.entity.Stock;
import com.smarterp.modules.inventory.repository.ProductRepository;
import com.smarterp.modules.inventory.repository.StockRepository;
import com.smarterp.modules.inventory.repository.ProductCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminInventoryService {

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final ProductCategoryRepository categoryRepository; // ✅ AGREGADO

    /**
     * 📊 DASHBOARD DE INVENTARIO
     */
    public Map<String, Object> getInventoryDashboard(String businessId) {
        log.info("📊 [ADMIN] Dashboard de inventario - Business: {}", businessId);

        Map<String, Object> dashboard = new HashMap<>();

        List<Product> allProducts = productRepository.findByBusinessId(businessId);
        List<Stock> allStock = stockRepository.findByBusinessId(businessId);

        log.info("📊 [ADMIN] Total productos: {}, Total stocks: {}", allProducts.size(), allStock.size());

        int totalProductos = allProducts.size();

        // Stock bajo (menos de minStock)
        List<Stock> stockBajo = allStock.stream()
                .filter(s -> s.getQuantity() < s.getMinStock())
                .collect(Collectors.toList());

        // Productos sin stock
        List<Stock> sinStock = allStock.stream()
                .filter(s -> s.getQuantity() == 0)
                .collect(Collectors.toList());

        // Valor total del inventario
        BigDecimal valorTotalInventario = allStock.stream()
                .map(stock -> {
                    Product product = allProducts.stream()
                            .filter(p -> p.getId().equals(stock.getProductId()))
                            .findFirst()
                            .orElse(null);
                    if (product != null) {
                        BigDecimal precio = product.getCostPrice() != null ? product.getCostPrice()
                                : product.getPrice();
                        return precio.multiply(BigDecimal.valueOf(stock.getQuantity()));
                    }
                    return BigDecimal.ZERO;
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Productos por categoría (CON NOMBRES REALES)
        Map<String, Object> productosPorCategoria = getProductsByCategoryWithNames(allProducts);

        // Top 5 productos con más stock
        List<Map<String, Object>> topProductosStock = allStock.stream()
                .sorted(Comparator.comparing(Stock::getQuantity).reversed())
                .limit(5)
                .map(stock -> {
                    Product product = allProducts.stream()
                            .filter(p -> p.getId().equals(stock.getProductId()))
                            .findFirst()
                            .orElse(null);

                    Map<String, Object> item = new HashMap<>();
                    if (product != null) {
                        item.put("productName", product.getName());
                        item.put("sku", product.getSku());
                        item.put("quantity", stock.getQuantity());
                        item.put("price", product.getPrice());
                        item.put("totalValue", product.getPrice().multiply(BigDecimal.valueOf(stock.getQuantity())));
                    }
                    return item;
                })
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toList());

        // Top 5 productos con menos stock
        List<Map<String, Object>> productosBajoStock = allStock.stream()
                .filter(s -> s.getQuantity() < s.getMinStock())
                .sorted(Comparator.comparing(Stock::getQuantity))
                .limit(5)
                .map(stock -> {
                    Product product = allProducts.stream()
                            .filter(p -> p.getId().equals(stock.getProductId()))
                            .findFirst()
                            .orElse(null);

                    Map<String, Object> item = new HashMap<>();
                    if (product != null) {
                        item.put("productName", product.getName());
                        item.put("sku", product.getSku());
                        item.put("quantity", stock.getQuantity());
                        item.put("minStock", stock.getMinStock());
                    }
                    return item;
                })
                .filter(item -> !item.isEmpty())
                .collect(Collectors.toList());

        dashboard.put("totalProductos", totalProductos);
        dashboard.put("stockBajo", stockBajo.size());
        dashboard.put("sinStock", sinStock.size());
        dashboard.put("valorTotalInventario", valorTotalInventario);
        dashboard.put("productosPorCategoria", productosPorCategoria);
        dashboard.put("topProductosStock", topProductosStock);
        dashboard.put("productosBajoStock", productosBajoStock);
        dashboard.put("alertasStock", stockBajo.size() + sinStock.size());

        log.info("✅ [ADMIN] Dashboard calculado: {} productos, {} alertas",
                totalProductos, stockBajo.size() + sinStock.size());

        return dashboard;
    }

    /**
     * 📋 LISTA DE PRODUCTOS CON STOCK (CON NOMBRE DE CATEGORÍA)
     */
    public Map<String, Object> getProductsWithStock(String businessId, int page, int size) {
        log.info("📋 [ADMIN] Obteniendo productos - Business: {} - Page: {}", businessId, page);

        List<Product> allProducts = productRepository.findByBusinessId(businessId);
        List<Stock> allStock = stockRepository.findByBusinessId(businessId);

        List<Map<String, Object>> productsWithStock = new ArrayList<>();

        for (Product product : allProducts) {
            Stock stock = allStock.stream()
                    .filter(s -> s.getProductId().equals(product.getId()))
                    .findFirst()
                    .orElse(null);

            // ✅ OBTENER NOMBRE DE LA CATEGORÍA
            String categoryName = "Sin categoría";
            if (product.getCategoryId() != null && !product.getCategoryId().isEmpty()) {
                try {
                    ProductCategory category = categoryRepository.findById(product.getCategoryId()).orElse(null);
                    if (category != null && category.getName() != null) {
                        categoryName = category.getName();
                    }
                } catch (Exception e) {
                    log.warn("⚠️ No se pudo obtener categoría {}: {}", product.getCategoryId(), e.getMessage());
                }
            }

            Map<String, Object> productMap = new HashMap<>();
            productMap.put("id", product.getId());
            productMap.put("name", product.getName());
            productMap.put("sku", product.getSku());
            productMap.put("price", product.getPrice());
            productMap.put("categoryId", product.getCategoryId());
            productMap.put("categoryName", categoryName); // ✅ NOMBRE REAL
            productMap.put("stock", stock != null ? stock.getQuantity() : 0);
            productMap.put("minStock", product.getMinStock() != null ? product.getMinStock() : 5);
            productMap.put("createdAt", product.getCreatedAt());

            productsWithStock.add(productMap);
        }

        // Paginación manual
        int start = page * size;
        int end = Math.min(start + size, productsWithStock.size());

        List<Map<String, Object>> paginatedProducts = start < productsWithStock.size()
                ? productsWithStock.subList(start, end)
                : new ArrayList<>();

        Map<String, Object> response = new HashMap<>();
        response.put("content", paginatedProducts);
        response.put("totalElements", productsWithStock.size());
        response.put("totalPages", (int) Math.ceil((double) productsWithStock.size() / size));
        response.put("currentPage", page);

        return response;
    }

    /**
     * 📊 ESTADÍSTICAS POR CATEGORÍA (CON NOMBRES REALES)
     */
    public Map<String, Object> getStatsByCategory(String businessId) {
        log.info("📊 [ADMIN] Estadísticas por categoría - Business: {}", businessId);

        List<Product> allProducts = productRepository.findByBusinessId(businessId);
        List<Stock> allStock = stockRepository.findByBusinessId(businessId);

        // Agrupar por categoryId
        Map<String, List<Product>> productsByCategory = allProducts.stream()
                .filter(p -> p.getCategoryId() != null && !p.getCategoryId().isEmpty())
                .collect(Collectors.groupingBy(Product::getCategoryId));

        List<Map<String, Object>> categoryStats = new ArrayList<>();

        for (Map.Entry<String, List<Product>> entry : productsByCategory.entrySet()) {
            String categoryId = entry.getKey();
            List<Product> products = entry.getValue();

            // ✅ OBTENER NOMBRE REAL DE LA CATEGORÍA
            String categoryName = "Categoría " + categoryId.substring(0, Math.min(8, categoryId.length()));
            try {
                ProductCategory category = categoryRepository.findById(categoryId).orElse(null);
                if (category != null && category.getName() != null) {
                    categoryName = category.getName();
                }
            } catch (Exception e) {
                log.warn("⚠️ No se pudo obtener categoría {}: {}", categoryId, e.getMessage());
            }

            long totalProductos = products.size();

            BigDecimal valorTotal = products.stream()
                    .map(product -> {
                        Stock stock = allStock.stream()
                                .filter(s -> s.getProductId().equals(product.getId()))
                                .findFirst()
                                .orElse(null);
                        if (stock != null) {
                            return product.getPrice().multiply(BigDecimal.valueOf(stock.getQuantity()));
                        }
                        return BigDecimal.ZERO;
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> stats = new HashMap<>();
            stats.put("categoryId", categoryId);
            stats.put("categoryName", categoryName); // ✅ NOMBRE REAL
            stats.put("totalProductos", totalProductos);
            stats.put("valorTotal", valorTotal);

            categoryStats.add(stats);
        }

        // Ordenar por valor total
        categoryStats.sort((a, b) -> {
            BigDecimal valueA = (BigDecimal) a.get("valorTotal");
            BigDecimal valueB = (BigDecimal) b.get("valorTotal");
            return valueB.compareTo(valueA);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("categories", categoryStats);
        response.put("totalCategories", categoryStats.size());

        return response;
    }

    /**
     * 🔍 Helper: Obtener productos por categoría con nombres reales
     */
    private Map<String, Object> getProductsByCategoryWithNames(List<Product> allProducts) {
        Map<String, Object> result = new HashMap<>();

        // Agrupar por categoryId
        Map<String, Long> productsByCategory = allProducts.stream()
                .filter(p -> p.getCategoryId() != null && !p.getCategoryId().isEmpty())
                .collect(Collectors.groupingBy(
                        Product::getCategoryId,
                        Collectors.counting()));

        // Obtener nombres reales
        Map<String, String> categoryNames = new HashMap<>();
        for (String categoryId : productsByCategory.keySet()) {
            try {
                ProductCategory category = categoryRepository.findById(categoryId).orElse(null);
                if (category != null && category.getName() != null) {
                    categoryNames.put(categoryId, category.getName());
                } else {
                    categoryNames.put(categoryId,
                            "Categoría " + categoryId.substring(0, Math.min(8, categoryId.length())));
                }
            } catch (Exception e) {
                categoryNames.put(categoryId, "Categoría " + categoryId.substring(0, Math.min(8, categoryId.length())));
            }
        }

        result.put("counts", productsByCategory);
        result.put("names", categoryNames);

        return result;
    }
}