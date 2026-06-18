package com.smarterp.modules.inventory.repository;

import com.smarterp.modules.inventory.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

        List<Product> findByBusinessId(String businessId);

        List<Product> findByBusinessIdAndCategoryId(String businessId, String categoryId);

        Optional<Product> findByIdAndBusinessId(String id, String businessId);

        boolean existsBySkuAndBusinessId(String sku, String businessId);

        @Query("SELECT p FROM Product p WHERE p.businessId = :businessId AND " +
                        "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(p.barcode) LIKE LOWER(CONCAT('%', :search, '%')))")
        List<Product> searchProducts(@Param("businessId") String businessId,
                        @Param("search") String search);
}