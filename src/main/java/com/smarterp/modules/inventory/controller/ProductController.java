package com.smarterp.modules.inventory.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.inventory.entity.Product;
import com.smarterp.modules.inventory.entity.ProductCategory;
import com.smarterp.modules.inventory.entity.Stock;
import com.smarterp.modules.inventory.repository.ProductCategoryRepository;
import com.smarterp.modules.inventory.repository.ProductRepository;
import com.smarterp.modules.inventory.repository.StockRepository;
import com.smarterp.modules.inventory.service.ProductImageService;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/inventory/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final ProductCategoryRepository categoryRepository;
    private final UserContext userContext;
    private final ProductImageService imageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Product>>> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String categoryId) {

        String businessId = userContext.getCurrentBusinessId();
        log.info("🔍 Buscando productos - search: {}, categoryId: {}", search, categoryId);

        List<Product> products;

        if (search != null && !search.isBlank()) {
            products = productRepository.searchProducts(businessId, search);
            log.info("✅ Búsqueda encontrada: {} productos", products.size());
        } else if (categoryId != null && !categoryId.isBlank()) {
            products = productRepository.findByBusinessIdAndCategoryId(businessId, categoryId);
            log.info("✅ Filtro por categoría: {} productos", products.size());
        } else {
            products = productRepository.findByBusinessId(businessId);
            log.info("✅ Todos los productos: {} productos", products.size());
        }

        products.forEach(product -> {
            stockRepository.findByProductIdAndBusinessId(product.getId(), businessId)
                    .ifPresent(stock -> product.setStock(stock.getQuantity()));

            if (product.getCategoryId() != null) {
                categoryRepository.findById(product.getCategoryId())
                        .ifPresent(category -> product.setCategoryName(category.getName()));
            }
        });

        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Product>> createProduct(@RequestBody Product product) {
        String businessId = userContext.getCurrentBusinessId();

        if (businessId == null || businessId.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("BusinessId no disponible"));
        }

        product.setBusinessId(businessId);

        if (product.getMinStock() == null)
            product.setMinStock(5);
        if (product.getUnit() == null)
            product.setUnit("UNIDAD");
        if (product.getStock() == null)
            product.setStock(0);

        Product saved = productRepository.save(product);

        Stock stock = Stock.builder()
                .productId(saved.getId())
                .productName(saved.getName())
                .productSku(saved.getSku())
                .businessId(businessId)
                .quantity(product.getStock())
                .minStock(product.getMinStock())
                .build();

        stockRepository.save(stock);

        return ResponseEntity.ok(ApiResponse.success("Producto creado", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> updateProduct(
            @PathVariable String id,
            @RequestBody Product product) {

        String businessId = userContext.getCurrentBusinessId();
        Product existing = productRepository.findById(id).orElseThrow();

        if (!existing.getBusinessId().equals(businessId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("No tiene permisos"));
        }

        existing.setName(product.getName());
        existing.setSku(product.getSku());
        existing.setPrice(product.getPrice());
        existing.setCostPrice(product.getCostPrice());
        existing.setMinStock(product.getMinStock());
        existing.setBarcode(product.getBarcode());
        existing.setDescription(product.getDescription());
        existing.setUnit(product.getUnit());
        existing.setCategoryId(product.getCategoryId());

        Product updated = productRepository.save(existing);

        stockRepository.findByProductIdAndBusinessId(id, businessId)
                .ifPresent(stock -> {
                    if (product.getStock() != null) {
                        stock.setQuantity(product.getStock());
                    }
                    stock.setProductName(existing.getName());
                    stock.setProductSku(existing.getSku());
                    if (product.getMinStock() != null) {
                        stock.setMinStock(product.getMinStock());
                    }
                    stockRepository.save(stock);
                });

        return ResponseEntity.ok(ApiResponse.success("Producto actualizado", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable String id) {
        String businessId = userContext.getCurrentBusinessId();
        Product product = productRepository.findById(id).orElseThrow();

        if (!product.getBusinessId().equals(businessId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("No tiene permisos"));
        }

        stockRepository.findByProductIdAndBusinessId(id, businessId)
                .ifPresent(stock -> stockRepository.delete(stock));

        productRepository.deleteById(id);

        return ResponseEntity.ok(ApiResponse.success("Producto eliminado", null));
    }
        /**
     * 📤 SUBIR IMAGEN
     */
    @PostMapping("/{id}/upload-image")
    public ResponseEntity<ApiResponse<Product>> uploadProductImage(
            @PathVariable String id,
            @RequestParam("image") MultipartFile file) {
        
        try {
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            // Eliminar imagen anterior si existe
            if (product.getImagePath() != null && !product.getImagePath().isEmpty()) {
                imageService.deleteImage(product.getImagePath());
            }

            // Subir nueva imagen
            String filename = imageService.uploadImage(file, id);
            
            // Actualizar producto
            product.setImagePath(filename);
            product.setImageUrl(imageService.getImageUrl(filename));
            
            Product updated = productRepository.save(product);
            return ResponseEntity.ok(ApiResponse.success("Imagen actualizada", updated));
        } catch (IOException e) {
            log.error("❌ Error al subir imagen: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}