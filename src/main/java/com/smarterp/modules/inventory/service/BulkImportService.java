package com.smarterp.modules.inventory.service;

import com.smarterp.modules.inventory.dto.BulkImportResult;
import com.smarterp.modules.inventory.entity.Product;
import com.smarterp.modules.inventory.entity.ProductCategory;
import com.smarterp.modules.inventory.entity.Stock;
import com.smarterp.modules.inventory.repository.ProductCategoryRepository;
import com.smarterp.modules.inventory.repository.ProductRepository;
import com.smarterp.modules.inventory.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkImportService {

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final ProductCategoryRepository categoryRepository;

    // ✅ Colores disponibles para categorías nuevas
    private static final String[] AVAILABLE_COLORS = {
            "#3B82F6", "#10B981", "#F59E0B", "#EF4444",
            "#8B5CF6", "#EC4899", "#14B8A6", "#F97316"
    };

    /**
     * Importar categorías desde CSV
     */
    @Transactional
    public BulkImportResult importCategories(MultipartFile file, String businessId) throws IOException {
        BulkImportResult result = new BulkImportResult();
        result.setTotal(0);
        result.setSuccess(0);
        result.setFailed(0);
        result.setErrors(new ArrayList<>());

        // ✅ Leer con UTF-8 y remover BOM si existe
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }

        List<String> lines = Arrays.asList(content.split("\r?\n"));

        if (lines.isEmpty()) {
            result.getErrors().add("Archivo vacío");
            return result;
        }

        log.info("📥 Importando categorías - {} líneas encontradas", lines.size());

        // Saltar header
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty())
                continue;

            result.setTotal(result.getTotal() + 1);

            try {
                // ✅ Parseo inteligente que maneja comillas
                String[] columns = parseCSVLine(line);

                if (columns.length < 1 || columns[0].trim().isEmpty()) {
                    result.getErrors().add("Línea " + (i + 1) + ": Nombre requerido");
                    result.setFailed(result.getFailed() + 1);
                    continue;
                }

                String name = columns[0].trim();
                String description = columns.length > 1 ? columns[1].trim() : "";
                String color = columns.length > 2 ? columns[2].trim() : generateRandomColor();

                // Validar que no exista
                if (categoryRepository.existsByNameAndBusinessId(name, businessId)) {
                    result.getErrors().add("Línea " + (i + 1) + ": Categoría '" + name + "' ya existe");
                    result.setFailed(result.getFailed() + 1);
                    continue;
                }

                ProductCategory category = ProductCategory.builder()
                        .businessId(businessId)
                        .name(name)
                        .description(description)
                        .color(color)
                        .isActive(true)
                        .build();

                categoryRepository.save(category);
                result.setSuccess(result.getSuccess() + 1);
                log.info("✅ Categoría importada: {}", name);

            } catch (Exception e) {
                result.getErrors().add("Línea " + (i + 1) + ": " + e.getMessage());
                result.setFailed(result.getFailed() + 1);
                log.error("Error en línea {}: {}", i + 1, e.getMessage());
            }
        }

        log.info("✅ Importación de categorías completada: {}/{} exitosas",
                result.getSuccess(), result.getTotal());
        return result;
    }

    /**
     * Importar productos desde CSV con creación automática de categorías
     */
    @Transactional
    public BulkImportResult importProducts(MultipartFile file, String businessId, boolean updateExisting)
            throws IOException {

        BulkImportResult result = new BulkImportResult();
        result.setTotal(0);
        result.setSuccess(0);
        result.setFailed(0);
        result.setErrors(new ArrayList<>());

        // ✅ Leer con UTF-8 y remover BOM si existe
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }

        List<String> lines = Arrays.asList(content.split("\r?\n"));

        if (lines.isEmpty()) {
            result.getErrors().add("Archivo vacío");
            return result;
        }

        log.info("📥 Importando productos - {} líneas - Actualizar existentes: {}",
                lines.size(), updateExisting);

        // ✅ Cargar categorías existentes (mapeo nombre -> ID)
        Map<String, String> categoryMap = new HashMap<>();
        categoryRepository.findByBusinessId(businessId)
                .forEach(cat -> categoryMap.put(cat.getName().toLowerCase().trim(), cat.getId()));

        int categoriesCreated = 0;

        // Saltar header
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty())
                continue;

            result.setTotal(result.getTotal() + 1);

            try {
                // ✅ Parseo inteligente que maneja comillas
                String[] columns = parseCSVLine(line);

                if (columns.length < 4) {
                    result.getErrors().add("Línea " + (i + 1) +
                            ": Formato inválido (mínimo 4 columnas: nombre, sku, precio, stock)");
                    result.setFailed(result.getFailed() + 1);
                    continue;
                }

                String name = columns[0].trim();
                String sku = columns[1].trim();
                String priceStr = columns[2].trim();
                String stockStr = columns[3].trim();
                String categoryName = columns.length > 4 ? columns[4].trim() : "";
                String description = columns.length > 5 ? columns[5].trim() : "";
                String barcode = columns.length > 6 ? columns[6].trim() : "";

                // Validar campos obligatorios
                if (name.isEmpty() || sku.isEmpty()) {
                    result.getErrors().add("Línea " + (i + 1) + ": Nombre y SKU son obligatorios");
                    result.setFailed(result.getFailed() + 1);
                    continue;
                }

                // Validar SKU único
                if (!updateExisting && productRepository.existsBySkuAndBusinessId(sku, businessId)) {
                    result.getErrors().add("Línea " + (i + 1) + ": SKU '" + sku + "' ya existe");
                    result.setFailed(result.getFailed() + 1);
                    continue;
                }

                BigDecimal price;
                int stock;

                try {
                    // ✅ Manejar precios con coma o punto decimal
                    priceStr = priceStr.replace(",", ".");
                    price = new BigDecimal(priceStr);
                    stock = Integer.parseInt(stockStr);

                    if (price.compareTo(BigDecimal.ZERO) < 0) {
                        result.getErrors().add("Línea " + (i + 1) + ": El precio no puede ser negativo");
                        result.setFailed(result.getFailed() + 1);
                        continue;
                    }
                    if (stock < 0) {
                        result.getErrors().add("Línea " + (i + 1) + ": El stock no puede ser negativo");
                        result.setFailed(result.getFailed() + 1);
                        continue;
                    }
                } catch (NumberFormatException e) {
                    result.getErrors().add("Línea " + (i + 1) +
                            ": Precio ('" + priceStr + "') o stock ('" + stockStr + "') inválido");
                    result.setFailed(result.getFailed() + 1);
                    continue;
                }

                // ✅ Buscar o crear categoría automáticamente
                String categoryId = null;
                if (!categoryName.isEmpty()) {
                    categoryId = categoryMap.get(categoryName.toLowerCase().trim());

                    // Si no existe, crearla automáticamente
                    if (categoryId == null) {
                        ProductCategory newCategory = ProductCategory.builder()
                                .businessId(businessId)
                                .name(categoryName)
                                .description("Categoría creada automáticamente desde importación")
                                .color(generateRandomColor())
                                .isActive(true)
                                .build();

                        categoryRepository.save(newCategory);
                        categoryId = newCategory.getId();
                        categoryMap.put(categoryName.toLowerCase().trim(), categoryId);
                        categoriesCreated++;

                        log.info("🆕 Categoría creada automáticamente: {}", categoryName);
                    }
                }

                Product product;

                if (updateExisting) {
                    // Buscar por SKU
                    Optional<Product> existingOpt = productRepository.findByIdAndBusinessId(sku, businessId);
                    if (existingOpt.isPresent()) {
                        product = existingOpt.get();
                        product.setName(name);
                        product.setPrice(price);
                        product.setCategoryId(categoryId);
                        if (!description.isEmpty())
                            product.setDescription(description);
                        if (!barcode.isEmpty())
                            product.setBarcode(barcode);
                        log.info("🔄 Producto actualizado: {} ({})", name, sku);
                    } else {
                        product = createNewProduct(businessId, name, sku, price, categoryId, description, barcode);
                    }
                } else {
                    product = createNewProduct(businessId, name, sku, price, categoryId, description, barcode);
                }

                productRepository.save(product);

                // ✅ Actualizar stock
                Stock stockEntity = stockRepository.findByProductIdAndBusinessId(product.getId(), businessId)
                        .orElseGet(() -> {
                            Stock newStock = Stock.builder()
                                    .productId(product.getId())
                                    .productName(product.getName())
                                    .productSku(product.getSku())
                                    .businessId(businessId)
                                    .quantity(0)
                                    .minStock(5)
                                    .build();
                            return stockRepository.save(newStock);
                        });

                stockEntity.setQuantity(stock);
                stockEntity.setProductName(product.getName());
                stockEntity.setProductSku(product.getSku());
                stockRepository.save(stockEntity);

                result.setSuccess(result.getSuccess() + 1);
                log.info("✅ Producto importado: {} ({}) - Stock: {}", name, sku, stock);

            } catch (Exception e) {
                result.getErrors().add("Línea " + (i + 1) + ": " + e.getMessage());
                result.setFailed(result.getFailed() + 1);
                log.error("Error en línea {}: {}", i + 1, e.getMessage(), e);
            }
        }

        if (categoriesCreated > 0) {
            log.info("✅ {} categorías creadas automáticamente durante la importación", categoriesCreated);
        }

        log.info("✅ Importación de productos completada: {}/{} exitosas",
                result.getSuccess(), result.getTotal());
        return result;
    }

    /**
     * Helper: Crear nuevo producto
     */
    private Product createNewProduct(String businessId, String name, String sku,
            BigDecimal price, String categoryId,
            String description, String barcode) {
        Product product = Product.builder()
                .businessId(businessId)
                .name(name)
                .sku(sku)
                .price(price)
                .categoryId(categoryId)
                .minStock(5)
                .unit("UNIDAD")
                .build();

        if (description != null && !description.isEmpty()) {
            product.setDescription(description);
        }
        if (barcode != null && !barcode.isEmpty()) {
            product.setBarcode(barcode);
        }

        return product;
    }

    /**
     * Obtener plantilla de categorías (con punto y coma para Excel en español)
     */
    public byte[] getCategoriesTemplate() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));

        // ✅ BOM para Excel + punto y coma como delimitador
        writer.print("\uFEFF");
        writer.println("nombre;descripcion;color");
        writer.println("Electrónica;\"Productos electrónicos y gadgets\";#3B82F6");
        writer.println("Ropa;\"Vestimenta y accesorios\";#10B981");
        writer.println("Alimentos;\"Productos alimenticios\";#F59E0B");
        writer.println("Hogar;\"Artículos para el hogar\";#8B5CF6");
        writer.println("Deportes;\"Equipamiento deportivo\";#EF4444");
        writer.println("Belleza;\"Productos de belleza y cuidado personal\";#EC4899");
        writer.println("Tecnología;\"Dispositivos y accesorios tech\";#14B8A6");

        writer.flush();
        return outputStream.toByteArray();
    }

    /**
     * Obtener plantilla de productos (con punto y coma para Excel en español)
     */
    public byte[] getProductsTemplate() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));

        // ✅ BOM para Excel + punto y coma como delimitador + columnas alineadas
        writer.print("\uFEFF");
        writer.println("nombre;sku;precio;stock;categoria;descripcion;codigo_barras");
        writer.println(
                "Audífonos Gamer HyperX;AUD-001;189.90;50;Electrónica;\"Audífonos gamer con micrófono\";740617269192");
        writer.println(
                "Teclado Mecánico RGB;TEC-001;299.00;30;Electrónica;\"Teclado mecánico con luces RGB\";740617311136");
        writer.println("Mouse Inalámbrico Logitech;MOU-001;89.90;100;Tecnología;\"Mouse inalámbrico 2.4GHz\";");
        writer.println("Camiseta Básica Blanca;CAM-001;49.90;100;Ropa;\"Camiseta 100% algodón talla M\";");
        writer.println("Pantalón Jeans Slim;JEA-001;129.90;50;Ropa;\"Jeans slim fit azul oscuro\";");
        writer.println("Arroz Costeño 1kg;ALI-001;5.50;200;Alimentos;\"Arroz blanco grano largo\";740617269193");
        writer.println("Aceite Primor 1L;ALI-002;8.90;150;Alimentos;\"Aceite vegetal comestible\";");
        writer.println("Balón de Fútbol FIFA;DEP-001;79.90;25;Deportes;\"Balón oficial talla 5\";");
        writer.println("Shampoo Sedal 400ml;BEL-001;18.50;80;Belleza;\"Shampoo para cabello graso\";");
        writer.println("Sartén Antiadherente 26cm;HOG-001;65.00;40;Hogar;\"Sartén con recubrimiento antiadherente\";");

        writer.flush();
        return outputStream.toByteArray();
    }

    /**
     * Validar archivo CSV
     */
    public Map<String, Object> validateFile(MultipartFile file, String type, String businessId)
            throws IOException {

        Map<String, Object> validation = new HashMap<>();
        List<String> errors = new ArrayList<>();

        // ✅ Leer con UTF-8 y remover BOM
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }

        List<String> lines = Arrays.asList(content.split("\r?\n"));

        if (lines.isEmpty()) {
            errors.add("Archivo vacío");
            validation.put("valid", false);
            validation.put("errors", errors);
            return validation;
        }

        // ✅ Detectar delimitador automáticamente
        char delimiter = lines.get(0).contains(";") ? ';' : ',';

        int validRows = 0;
        int invalidRows = 0;

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty())
                continue;

            try {
                String[] columns = line.split(delimiter == ';' ? ";" : ",");

                if (type.equals("products")) {
                    if (columns.length < 4) {
                        errors.add("Línea " + (i + 1) + ": Faltan columnas (mínimo: nombre, sku, precio, stock)");
                        invalidRows++;
                    } else if (columns[0].trim().isEmpty() || columns[1].trim().isEmpty()) {
                        errors.add("Línea " + (i + 1) + ": Nombre y SKU son obligatorios");
                        invalidRows++;
                    } else {
                        // Validar precio y stock
                        String priceStr = columns[2].trim().replace(",", ".");
                        BigDecimal price = new BigDecimal(priceStr);
                        int stock = Integer.parseInt(columns[3].trim());

                        if (price.compareTo(BigDecimal.ZERO) < 0) {
                            errors.add("Línea " + (i + 1) + ": El precio no puede ser negativo");
                            invalidRows++;
                        } else if (stock < 0) {
                            errors.add("Línea " + (i + 1) + ": El stock no puede ser negativo");
                            invalidRows++;
                        } else {
                            validRows++;
                        }
                    }
                } else if (type.equals("categories")) {
                    if (columns.length < 1 || columns[0].trim().isEmpty()) {
                        errors.add("Línea " + (i + 1) + ": Nombre requerido");
                        invalidRows++;
                    } else {
                        validRows++;
                    }
                }
            } catch (NumberFormatException e) {
                errors.add("Línea " + (i + 1) + ": Precio o stock inválido (deben ser números)");
                invalidRows++;
            } catch (Exception e) {
                errors.add("Línea " + (i + 1) + ": " + e.getMessage());
                invalidRows++;
            }
        }

        validation.put("valid", errors.isEmpty());
        validation.put("totalRows", lines.size() - 1);
        validation.put("validRows", validRows);
        validation.put("invalidRows", invalidRows);
        validation.put("errors", errors);

        return validation;
    }

    /**
     * Parseo inteligente de CSV que maneja:
     * - Punto y coma (;) - Excel en español
     * - Coma (,) - CSV estándar
     * - Comillas para campos con delimitadores
     */
    private String[] parseCSVLine(String line) {
        // ✅ Detectar automáticamente el delimitador
        char delimiter = line.contains(";") ? ';' : ',';

        List<String> columns = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                // Verificar comillas escapadas ("")
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++; // Saltar la segunda comilla
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == delimiter && !inQuotes) {
                columns.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        columns.add(current.toString().trim());

        return columns.toArray(new String[0]);
    }

    /**
     * ✅ Generar color aleatorio para categorías nuevas
     */
    private String generateRandomColor() {
        return AVAILABLE_COLORS[(int) (Math.random() * AVAILABLE_COLORS.length)];
    }
}