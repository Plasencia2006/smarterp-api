package com.smarterp.modules.inventory.repository;

import com.smarterp.modules.inventory.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, String> {

    Optional<Stock> findByProductIdAndBusinessId(String productId, String businessId);

    List<Stock> findByBusinessId(String businessId);

    @Query("SELECT s FROM Stock s WHERE s.businessId = :businessId AND s.quantity <= s.minStock")
    List<Stock> findLowStock(@Param("businessId") String businessId);

    @Query("SELECT s FROM Stock s WHERE s.businessId = :businessId AND " +
            "(LOWER(s.productName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.productSku) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Stock> searchStock(@Param("businessId") String businessId, @Param("search") String search);
}