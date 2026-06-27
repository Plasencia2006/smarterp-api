package com.smarterp.modules.admin.service;

import com.smarterp.modules.admin.dto.ReportRequest;
import com.smarterp.modules.sales.entity.Quote;
import com.smarterp.modules.sales.repository.QuoteRepository;
import com.smarterp.modules.inventory.entity.Product;
import com.smarterp.modules.inventory.entity.Stock;
import com.smarterp.modules.inventory.entity.ProductCategory;
import com.smarterp.modules.inventory.repository.ProductRepository;
import com.smarterp.modules.inventory.repository.StockRepository;
import com.smarterp.modules.inventory.repository.ProductCategoryRepository;
import com.smarterp.modules.cashier.entity.CashRegister;
import com.smarterp.modules.cashier.repository.CashRegisterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final QuoteRepository quoteRepository;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final CashRegisterRepository registerRepository;
    private final ProductCategoryRepository categoryRepository;

    /**
     * 📊 GENERAR REPORTE SEGÚN TIPO
     */
    public ByteArrayResource generateReport(ReportRequest request) {
        log.info("📊 Generando reporte: {} - Formato: {}", request.getReportType(), request.getFormat());

        return switch (request.getReportType().toUpperCase()) {
            case "SALES" -> generateSalesReport(request);
            case "INVENTORY" -> generateInventoryReport(request);
            case "CASH" -> generateCashReport(request);
            case "PRODUCTS" -> generateProductsReport(request);
            case "CUSTOMERS" -> generateCustomersReport(request);
            default -> throw new IllegalArgumentException("Tipo de reporte no soportado: " + request.getReportType());
        };
    }

    /**
     * 💰 REPORTE DE VENTAS
     */
    private ByteArrayResource generateSalesReport(ReportRequest request) {
        log.info("💰 Generando reporte de ventas");

        // ✅ Obtener todas las cotizaciones y filtrar en memoria
        List<Quote> allQuotes = quoteRepository.findByBusinessIdOrderByCreatedAtDesc(request.getBusinessId());

        List<Quote> quotes = allQuotes.stream()
                .filter(q -> q.getCreatedAt() != null)
                .filter(q -> !q.getCreatedAt().isBefore(request.getStartDate()))
                .filter(q -> !q.getCreatedAt().isAfter(request.getEndDate()))
                .collect(Collectors.toList());

        if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
            return generateSalesExcel(quotes);
        } else {
            return generateSalesCSV(quotes);
        }
    }

    /**
     * 📦 REPORTE DE INVENTARIO
     */
    private ByteArrayResource generateInventoryReport(ReportRequest request) {
        log.info("📦 Generando reporte de inventario");

        List<Stock> stocks = stockRepository.findByBusinessId(request.getBusinessId());
        List<Product> products = productRepository.findByBusinessId(request.getBusinessId());

        if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
            return generateInventoryExcel(products, stocks);
        } else {
            return generateInventoryCSV(products, stocks);
        }
    }

    /**
     * 💵 REPORTE DE CAJA
     */
    private ByteArrayResource generateCashReport(ReportRequest request) {
        log.info("💵 Generando reporte de caja");

        // ✅ Obtener todas las cajas y filtrar en memoria
        List<CashRegister> allRegisters = registerRepository
                .findByBusinessIdOrderByOpeningTimeDesc(request.getBusinessId());

        List<CashRegister> registers = allRegisters.stream()
                .filter(r -> r.getOpeningTime() != null)
                .filter(r -> !r.getOpeningTime().isBefore(request.getStartDate()))
                .filter(r -> !r.getOpeningTime().isAfter(request.getEndDate()))
                .collect(Collectors.toList());

        if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
            return generateCashExcel(registers);
        } else {
            return generateCashCSV(registers);
        }
    }

    /**
     * 🛍️ REPORTE DE PRODUCTOS
     */
    private ByteArrayResource generateProductsReport(ReportRequest request) {
        log.info("🛍️ Generando reporte de productos");

        List<Product> products = productRepository.findByBusinessId(request.getBusinessId());

        if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
            return generateProductsExcel(products);
        } else {
            return generateProductsCSV(products);
        }
    }

    /**
     * 👥 REPORTE DE CLIENTES
     */
    private ByteArrayResource generateCustomersReport(ReportRequest request) {
        log.info("👥 Generando reporte de clientes");

        List<Quote> allQuotes = quoteRepository.findByBusinessIdOrderByCreatedAtDesc(request.getBusinessId());

        List<Quote> quotes = allQuotes.stream()
                .filter(q -> q.getCreatedAt() != null)
                .filter(q -> !q.getCreatedAt().isBefore(request.getStartDate()))
                .filter(q -> !q.getCreatedAt().isAfter(request.getEndDate()))
                .collect(Collectors.toList());

        // Extraer clientes únicos
        Map<String, Quote> uniqueCustomers = new LinkedHashMap<>();
        for (Quote quote : quotes) {
            String customerKey = quote.getCustomerName() + "_" + quote.getCustomerDocument();
            if (!uniqueCustomers.containsKey(customerKey)) {
                uniqueCustomers.put(customerKey, quote);
            }
        }

        if ("EXCEL".equalsIgnoreCase(request.getFormat())) {
            return generateCustomersExcel(new ArrayList<>(uniqueCustomers.values()));
        } else {
            return generateCustomersCSV(new ArrayList<>(uniqueCustomers.values()));
        }
    }

    // ==================== HELPERS ====================

    /**
     * ✅ Obtener nombre de categoría por ID
     */
    private String getCategoryName(String categoryId) {
        if (categoryId == null || categoryId.isEmpty()) {
            return "Sin categoría";
        }
        try {
            ProductCategory category = categoryRepository.findById(categoryId).orElse(null);
            if (category != null && category.getName() != null) {
                return category.getName();
            }
        } catch (Exception e) {
            log.debug("No se pudo obtener categoría: {}", categoryId);
        }
        return "Sin categoría";
    }

    private ByteArrayResource createResource(String content, String filename) {
        return new ByteArrayResource(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    // ==================== GENERADORES CSV ====================

    private ByteArrayResource generateSalesCSV(List<Quote> quotes) {
        StringBuilder csv = new StringBuilder();
        csv.append("\uFEFF"); // BOM UTF-8
        csv.append("Número,Cliente,Documento,Estado,Subtotal,IGV,Total,Fecha,Método de Pago\n");

        BigDecimal totalVentas = BigDecimal.ZERO;

        for (Quote quote : quotes) {
            csv.append(quote.getQuoteNumber()).append(",");
            csv.append(quote.getCustomerName()).append(",");
            csv.append(quote.getCustomerDocument() != null ? quote.getCustomerDocument() : "N/A").append(",");
            csv.append(quote.getStatus()).append(",");
            csv.append(quote.getSubtotal() != null ? quote.getSubtotal() : "0").append(",");
            csv.append(quote.getIgv() != null ? quote.getIgv() : "0").append(",");
            csv.append(quote.getTotal() != null ? quote.getTotal() : "0").append(",");
            csv.append(quote.getCreatedAt() != null
                    ? quote.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    : "N/A").append(",");
            csv.append(quote.getPaymentMethod() != null ? quote.getPaymentMethod() : "N/A").append("\n");

            if (quote.getTotal() != null) {
                totalVentas = totalVentas.add(quote.getTotal());
            }
        }

        csv.append("\nTOTAL VENTAS,,,,,,").append(totalVentas).append("\n");

        return createResource(csv.toString(), "ventas_reporte.csv");
    }

    private ByteArrayResource generateInventoryCSV(List<Product> products, List<Stock> stocks) {
        StringBuilder csv = new StringBuilder();
        csv.append("\uFEFF"); // BOM UTF-8
        csv.append("Producto,SKU,Categoría,Precio,Costo,Stock Actual,Stock Mínimo,Valor Inventario\n");

        Map<String, Stock> stockMap = new HashMap<>();
        for (Stock stock : stocks) {
            stockMap.put(stock.getProductId(), stock);
        }

        for (Product product : products) {
            Stock stock = stockMap.get(product.getId());
            int stockQty = stock != null ? stock.getQuantity() : 0;
            BigDecimal valorInventario = product.getPrice().multiply(BigDecimal.valueOf(stockQty));

            csv.append(product.getName()).append(",");
            csv.append(product.getSku()).append(",");
            csv.append(getCategoryName(product.getCategoryId())).append(",");
            csv.append(product.getPrice()).append(",");
            csv.append(product.getCostPrice() != null ? product.getCostPrice() : "0").append(",");
            csv.append(stockQty).append(",");
            csv.append(product.getMinStock() != null ? product.getMinStock() : "0").append(",");
            csv.append(valorInventario).append("\n");
        }

        return createResource(csv.toString(), "inventario_reporte.csv");
    }

    private ByteArrayResource generateCashCSV(List<CashRegister> registers) {
        StringBuilder csv = new StringBuilder();
        csv.append("\uFEFF"); // BOM UTF-8
        csv.append("Cajero,Estado,Apertura,Cierre,Monto Inicial,Efectivo Esperado,Efectivo Final,Diferencia\n");

        for (CashRegister register : registers) {
            csv.append(register.getUserName() != null ? register.getUserName() : "N/A").append(",");
            csv.append(register.getStatus()).append(",");
            csv.append(register.getOpeningTime() != null
                    ? register.getOpeningTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    : "N/A").append(",");
            csv.append(register.getClosingTime() != null
                    ? register.getClosingTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    : "Aún abierta").append(",");
            csv.append(register.getInitialAmount() != null ? register.getInitialAmount() : "0").append(",");
            csv.append(register.getExpectedCash() != null ? register.getExpectedCash() : "0").append(",");
            csv.append(register.getFinalCash() != null ? register.getFinalCash() : "0").append(",");
            csv.append(register.getCashDifference() != null ? register.getCashDifference() : "0").append("\n");
        }

        return createResource(csv.toString(), "caja_reporte.csv");
    }

    private ByteArrayResource generateProductsCSV(List<Product> products) {
        StringBuilder csv = new StringBuilder();
        csv.append("\uFEFF"); // BOM UTF-8
        csv.append("Producto,SKU,Descripción,Precio,Costo,Categoría,Unidad,Stock Mínimo\n");

        for (Product product : products) {
            csv.append(product.getName()).append(",");
            csv.append(product.getSku()).append(",");
            csv.append(product.getDescription() != null ? product.getDescription().replace(",", " ") : "").append(",");
            csv.append(product.getPrice()).append(",");
            csv.append(product.getCostPrice() != null ? product.getCostPrice() : "0").append(",");
            csv.append(getCategoryName(product.getCategoryId())).append(",");
            csv.append(product.getUnit() != null ? product.getUnit() : "UNIDAD").append(",");
            csv.append(product.getMinStock() != null ? product.getMinStock() : "0").append("\n");
        }

        return createResource(csv.toString(), "productos_reporte.csv");
    }

    private ByteArrayResource generateCustomersCSV(List<Quote> quotes) {
        StringBuilder csv = new StringBuilder();
        csv.append("\uFEFF"); // BOM UTF-8
        csv.append("Cliente,Documento,Total Compras,Cantidad Compras,Última Compra\n");

        Map<String, CustomerStats> customerStats = new LinkedHashMap<>();

        for (Quote quote : quotes) {
            String key = quote.getCustomerName() + "_"
                    + (quote.getCustomerDocument() != null ? quote.getCustomerDocument() : "");

            customerStats.computeIfAbsent(key, k -> new CustomerStats(
                    quote.getCustomerName(),
                    quote.getCustomerDocument()));

            CustomerStats stats = customerStats.get(key);
            if (quote.getTotal() != null) {
                stats.totalPurchases = stats.totalPurchases.add(quote.getTotal());
            }
            stats.purchaseCount++;
            if (quote.getCreatedAt() != null
                    && (stats.lastPurchase == null || quote.getCreatedAt().isAfter(stats.lastPurchase))) {
                stats.lastPurchase = quote.getCreatedAt();
            }
        }

        for (CustomerStats stats : customerStats.values()) {
            csv.append(stats.name).append(",");
            csv.append(stats.document != null ? stats.document : "N/A").append(",");
            csv.append(stats.totalPurchases).append(",");
            csv.append(stats.purchaseCount).append(",");
            csv.append(stats.lastPurchase != null
                    ? stats.lastPurchase.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    : "N/A").append("\n");
        }

        return createResource(csv.toString(), "clientes_reporte.csv");
    }

    // ==================== GENERADORES EXCEL ====================

    private ByteArrayResource generateSalesExcel(List<Quote> quotes) {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Ventas");

            Row headerRow = sheet.createRow(0);
            String[] headers = { "Número", "Cliente", "Documento", "Estado", "Subtotal", "IGV", "Total", "Fecha",
                    "Método Pago" };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(createHeaderStyle(workbook));
            }

            int rowNum = 1;
            for (Quote quote : quotes) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(quote.getQuoteNumber());
                row.createCell(1).setCellValue(quote.getCustomerName());
                row.createCell(2)
                        .setCellValue(quote.getCustomerDocument() != null ? quote.getCustomerDocument() : "N/A");
                row.createCell(3).setCellValue(quote.getStatus().toString());
                row.createCell(4).setCellValue(quote.getSubtotal() != null ? quote.getSubtotal().doubleValue() : 0);
                row.createCell(5).setCellValue(quote.getIgv() != null ? quote.getIgv().doubleValue() : 0);
                row.createCell(6).setCellValue(quote.getTotal() != null ? quote.getTotal().doubleValue() : 0);
                row.createCell(7)
                        .setCellValue(quote.getCreatedAt() != null
                                ? quote.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                                : "N/A");
                row.createCell(8).setCellValue(quote.getPaymentMethod() != null ? quote.getPaymentMethod() : "N/A");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayResource(out.toByteArray());

        } catch (Exception e) {
            log.error("❌ Error generando Excel de ventas", e);
            throw new RuntimeException("Error generando Excel: " + e.getMessage());
        }
    }

    private ByteArrayResource generateInventoryExcel(List<Product> products, List<Stock> stocks) {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Inventario");

            Row headerRow = sheet.createRow(0);
            String[] headers = { "Producto", "SKU", "Categoría", "Precio", "Costo", "Stock", "Mínimo", "Valor Total" };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(createHeaderStyle(workbook));
            }

            Map<String, Stock> stockMap = new HashMap<>();
            for (Stock stock : stocks) {
                stockMap.put(stock.getProductId(), stock);
            }

            int rowNum = 1;
            for (Product product : products) {
                Row row = sheet.createRow(rowNum++);
                Stock stock = stockMap.get(product.getId());
                int stockQty = stock != null ? stock.getQuantity() : 0;
                BigDecimal valorTotal = product.getPrice().multiply(BigDecimal.valueOf(stockQty));

                row.createCell(0).setCellValue(product.getName());
                row.createCell(1).setCellValue(product.getSku());
                row.createCell(2).setCellValue(getCategoryName(product.getCategoryId()));
                row.createCell(3).setCellValue(product.getPrice().doubleValue());
                row.createCell(4)
                        .setCellValue(product.getCostPrice() != null ? product.getCostPrice().doubleValue() : 0);
                row.createCell(5).setCellValue(stockQty);
                row.createCell(6).setCellValue(product.getMinStock() != null ? product.getMinStock() : 0);
                row.createCell(7).setCellValue(valorTotal.doubleValue());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayResource(out.toByteArray());

        } catch (Exception e) {
            log.error("❌ Error generando Excel de inventario", e);
            throw new RuntimeException("Error generando Excel: " + e.getMessage());
        }
    }

    private ByteArrayResource generateCashExcel(List<CashRegister> registers) {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Caja");

            Row headerRow = sheet.createRow(0);
            String[] headers = { "Cajero", "Estado", "Apertura", "Cierre", "Inicial", "Esperado", "Final",
                    "Diferencia" };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(createHeaderStyle(workbook));
            }

            int rowNum = 1;
            for (CashRegister register : registers) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(register.getUserName() != null ? register.getUserName() : "N/A");
                row.createCell(1).setCellValue(register.getStatus().toString());
                row.createCell(2)
                        .setCellValue(register.getOpeningTime() != null
                                ? register.getOpeningTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                                : "N/A");
                row.createCell(3)
                        .setCellValue(register.getClosingTime() != null
                                ? register.getClosingTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                                : "Aún abierta");
                row.createCell(4).setCellValue(
                        register.getInitialAmount() != null ? register.getInitialAmount().doubleValue() : 0);
                row.createCell(5).setCellValue(
                        register.getExpectedCash() != null ? register.getExpectedCash().doubleValue() : 0);
                row.createCell(6)
                        .setCellValue(register.getFinalCash() != null ? register.getFinalCash().doubleValue() : 0);
                row.createCell(7).setCellValue(
                        register.getCashDifference() != null ? register.getCashDifference().doubleValue() : 0);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayResource(out.toByteArray());

        } catch (Exception e) {
            log.error("❌ Error generando Excel de caja", e);
            throw new RuntimeException("Error generando Excel: " + e.getMessage());
        }
    }

    private ByteArrayResource generateProductsExcel(List<Product> products) {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Productos");

            Row headerRow = sheet.createRow(0);
            String[] headers = { "Producto", "SKU", "Descripción", "Precio", "Costo", "Categoría", "Unidad", "Mínimo" };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(createHeaderStyle(workbook));
            }

            int rowNum = 1;
            for (Product product : products) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(product.getName());
                row.createCell(1).setCellValue(product.getSku());
                row.createCell(2).setCellValue(product.getDescription() != null ? product.getDescription() : "");
                row.createCell(3).setCellValue(product.getPrice().doubleValue());
                row.createCell(4)
                        .setCellValue(product.getCostPrice() != null ? product.getCostPrice().doubleValue() : 0);
                row.createCell(5).setCellValue(getCategoryName(product.getCategoryId()));
                row.createCell(6).setCellValue(product.getUnit() != null ? product.getUnit() : "UNIDAD");
                row.createCell(7).setCellValue(product.getMinStock() != null ? product.getMinStock() : 0);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayResource(out.toByteArray());

        } catch (Exception e) {
            log.error("❌ Error generando Excel de productos", e);
            throw new RuntimeException("Error generando Excel: " + e.getMessage());
        }
    }

    private ByteArrayResource generateCustomersExcel(List<Quote> quotes) {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Clientes");

            Row headerRow = sheet.createRow(0);
            String[] headers = { "Cliente", "Documento", "Total Compras", "Cantidad Compras", "Última Compra" };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(createHeaderStyle(workbook));
            }

            Map<String, CustomerStats> customerStats = new LinkedHashMap<>();
            for (Quote quote : quotes) {
                String key = quote.getCustomerName() + "_"
                        + (quote.getCustomerDocument() != null ? quote.getCustomerDocument() : "");

                customerStats.computeIfAbsent(key, k -> new CustomerStats(
                        quote.getCustomerName(),
                        quote.getCustomerDocument()));

                CustomerStats stats = customerStats.get(key);
                if (quote.getTotal() != null) {
                    stats.totalPurchases = stats.totalPurchases.add(quote.getTotal());
                }
                stats.purchaseCount++;
                if (quote.getCreatedAt() != null
                        && (stats.lastPurchase == null || quote.getCreatedAt().isAfter(stats.lastPurchase))) {
                    stats.lastPurchase = quote.getCreatedAt();
                }
            }

            int rowNum = 1;
            for (CustomerStats stats : customerStats.values()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(stats.name);
                row.createCell(1).setCellValue(stats.document != null ? stats.document : "N/A");
                row.createCell(2).setCellValue(stats.totalPurchases.doubleValue());
                row.createCell(3).setCellValue(stats.purchaseCount);
                row.createCell(4)
                        .setCellValue(stats.lastPurchase != null
                                ? stats.lastPurchase.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                                : "N/A");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayResource(out.toByteArray());

        } catch (Exception e) {
            log.error("❌ Error generando Excel de clientes", e);
            throw new RuntimeException("Error generando Excel: " + e.getMessage());
        }
    }

    // Inner class for customer stats
    private static class CustomerStats {
        String name;
        String document;
        BigDecimal totalPurchases = BigDecimal.ZERO;
        int purchaseCount = 0;
        LocalDateTime lastPurchase;

        CustomerStats(String name, String document) {
            this.name = name;
            this.document = document;
        }
    }
}