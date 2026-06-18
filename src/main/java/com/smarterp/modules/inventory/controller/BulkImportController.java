package com.smarterp.modules.inventory.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.inventory.dto.BulkImportResult;
import com.smarterp.modules.inventory.service.BulkImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/inventory/bulk")
@RequiredArgsConstructor
@Slf4j
public class BulkImportController {

    private final BulkImportService bulkImportService;
    private final UserContext userContext;

    /**
     * Importar categorías desde CSV
     */
    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<BulkImportResult>> importCategories(
            @RequestParam("file") MultipartFile file) throws IOException {

        String businessId = userContext.getCurrentBusinessId();
        log.info("📥 Importando categorías - Archivo: {}", file.getOriginalFilename());

        BulkImportResult result = bulkImportService.importCategories(file, businessId);

        // ✅ CORRECTO: ApiResponse.success(message, data)
        return ResponseEntity.ok(ApiResponse.success("Categorías importadas", result));
    }

    /**
     * Importar productos desde CSV
     */
    @PostMapping("/products")
    public ResponseEntity<ApiResponse<BulkImportResult>> importProducts(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "updateExisting", defaultValue = "false") boolean updateExisting)
            throws IOException {

        String businessId = userContext.getCurrentBusinessId();
        log.info("📥 Importando productos - Archivo: {} - Actualizar existentes: {}",
                file.getOriginalFilename(), updateExisting);

        BulkImportResult result = bulkImportService.importProducts(file, businessId, updateExisting);

        // ✅ CORRECTO: ApiResponse.success(message, data)
        return ResponseEntity.ok(ApiResponse.success("Productos importados", result));
    }

    /**
     * Descargar plantilla de categorías
     */
    @GetMapping("/templates/categories")
    public ResponseEntity<byte[]> downloadCategoriesTemplate() {
        byte[] template = bulkImportService.getCategoriesTemplate();

        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=\"plantilla_categorias.csv\"")
                .body(template);
    }

    /**
     * Descargar plantilla de productos
     */
    @GetMapping("/templates/products")
    public ResponseEntity<byte[]> downloadProductsTemplate() {
        byte[] template = bulkImportService.getProductsTemplate();

        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=\"plantilla_productos.csv\"")
                .body(template);
    }

    /**
     * Validar archivo CSV sin importar
     */
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") String type) throws IOException {

        String businessId = userContext.getCurrentBusinessId();
        log.info("🔍 Validando archivo - Tipo: {}", type);

        Map<String, Object> validation = bulkImportService.validateFile(file, type, businessId);

        // ✅ CORRECTO: ApiResponse.success(message, data)
        return ResponseEntity.ok(ApiResponse.success("Validación completada", validation));
    }
}