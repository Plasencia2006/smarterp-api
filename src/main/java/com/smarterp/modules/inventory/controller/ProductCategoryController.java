package com.smarterp.modules.inventory.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.inventory.entity.ProductCategory;
import com.smarterp.modules.inventory.repository.ProductCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory/categories")
@RequiredArgsConstructor
@Slf4j
public class ProductCategoryController {

    private final ProductCategoryRepository categoryRepository;
    private final UserContext userContext;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductCategory>>> getCategories(
            @RequestParam(required = false) String search) {
        String businessId = userContext.getCurrentBusinessId();

        log.info("📋 Obteniendo categorías para business: {}", businessId);

        List<ProductCategory> categories;
        if (search != null && !search.isBlank()) {
            categories = categoryRepository.searchCategories(businessId, search);
        } else {
            categories = categoryRepository.findByBusinessIdAndIsActive(businessId, true);
        }

        log.info("✅ {} categorías encontradas", categories.size());

        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductCategory>> getCategory(@PathVariable String id) {
        String businessId = userContext.getCurrentBusinessId();
        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

        if (!category.getBusinessId().equals(businessId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("No tiene permisos"));
        }

        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductCategory>> createCategory(
            @RequestBody ProductCategory category) {
        String businessId = userContext.getCurrentBusinessId();

        log.info("➕ Creando categoría: {} para business: {}", category.getName(), businessId);

        // ✅ VALIDAR que el businessId no sea null
        if (businessId == null || businessId.isBlank()) {
            log.error("❌ BusinessId es null o vacío");
            return ResponseEntity.badRequest().body(ApiResponse.error("BusinessId no disponible"));
        }

        // Validar nombre único
        if (categoryRepository.existsByNameAndBusinessId(category.getName(), businessId)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Ya existe una categoría con ese nombre"));
        }

        // ✅ ASIGNAR businessId explícitamente
        category.setBusinessId(businessId);

        // Valores por defecto
        if (category.getIsActive() == null) {
            category.setIsActive(true);
        }
        if (category.getColor() == null || category.getColor().isBlank()) {
            category.setColor("#3B82F6");
        }

        ProductCategory saved = categoryRepository.save(category);

        log.info("✅ Categoría creada: {} (ID: {})", saved.getName(), saved.getId());

        return ResponseEntity.ok(ApiResponse.success("Categoría creada", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductCategory>> updateCategory(
            @PathVariable String id,
            @RequestBody ProductCategory category) {
        String businessId = userContext.getCurrentBusinessId();

        ProductCategory existing = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

        if (!existing.getBusinessId().equals(businessId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("No tiene permisos"));
        }

        // Validar nombre único si cambió
        if (!existing.getName().equals(category.getName()) &&
                categoryRepository.existsByNameAndBusinessId(category.getName(), businessId)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Ya existe una categoría con ese nombre"));
        }

        existing.setName(category.getName());
        existing.setDescription(category.getDescription());
        existing.setColor(category.getColor());
        existing.setIcon(category.getIcon());
        existing.setParentId(category.getParentId());
        if (category.getIsActive() != null) {
            existing.setIsActive(category.getIsActive());
        }

        ProductCategory updated = categoryRepository.save(existing);

        log.info("✅ Categoría actualizada: {}", updated.getName());

        return ResponseEntity.ok(ApiResponse.success("Categoría actualizada", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable String id) {
        String businessId = userContext.getCurrentBusinessId();

        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada"));

        if (!category.getBusinessId().equals(businessId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("No tiene permisos"));
        }

        categoryRepository.delete(category);

        log.info("🗑️ Categoría eliminada: {}", category.getName());

        return ResponseEntity.ok(ApiResponse.success("Categoría eliminada", null));
    }
}