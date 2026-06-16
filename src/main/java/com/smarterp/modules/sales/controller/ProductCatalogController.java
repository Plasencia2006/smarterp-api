package com.smarterp.modules.sales.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.modules.inventory.entity.Product;
import com.smarterp.modules.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sales/products")
@RequiredArgsConstructor
public class ProductCatalogController {

    private final ProductRepository productRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<Product>>> getProducts() {
        return ResponseEntity.ok(ApiResponse.success(productRepository.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getProduct(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(productRepository.findById(id).orElseThrow()));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<java.util.List<com.smarterp.modules.inventory.entity.ProductCategory>>> getCategories() {
        // Implementar repositorio de categorías
        return ResponseEntity.ok(ApiResponse.success(java.util.Collections.emptyList()));
    }
}