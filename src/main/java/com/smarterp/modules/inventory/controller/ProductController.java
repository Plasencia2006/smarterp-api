package com.smarterp.modules.inventory.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.inventory.entity.Product;
import com.smarterp.modules.inventory.repository.ProductRepository;
import com.smarterp.shared.entity.Business;
import com.smarterp.shared.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;
    private final BusinessRepository businessRepository;
    private final UserContext userContext;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Product>>> getProducts() {
        String businessId = userContext.getCurrentBusinessId();
        List<Product> products = productRepository.findByBusinessId(businessId);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Product>> createProduct(@RequestBody Product product) {
        String businessId = userContext.getCurrentBusinessId();
        Business business = businessRepository.findById(businessId).orElseThrow();
        product.setBusiness(business);
        return ResponseEntity.ok(ApiResponse.success("Producto creado", productRepository.save(product)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getProduct(@PathVariable String id) {
        String businessId = userContext.getCurrentBusinessId();
        Product product = productRepository.findById(id).orElseThrow();

        if (!product.getBusiness().getId().equals(businessId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("No tiene permisos"));
        }

        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> updateProduct(@PathVariable String id, @RequestBody Product product) {
        String businessId = userContext.getCurrentBusinessId();
        Product existing = productRepository.findById(id).orElseThrow();

        if (!existing.getBusiness().getId().equals(businessId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("No tiene permisos"));
        }

        existing.setName(product.getName());
        existing.setSku(product.getSku());
        existing.setPrice(product.getPrice());
        existing.setCostPrice(product.getCostPrice());

        return ResponseEntity.ok(ApiResponse.success("Producto actualizado", productRepository.save(existing)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable String id) {
        String businessId = userContext.getCurrentBusinessId();
        Product product = productRepository.findById(id).orElseThrow();

        if (!product.getBusiness().getId().equals(businessId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("No tiene permisos"));
        }

        productRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Producto eliminado", null));
    }
}