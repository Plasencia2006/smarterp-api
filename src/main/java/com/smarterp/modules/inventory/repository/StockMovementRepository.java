package com.smarterp.modules.inventory.repository;

import com.smarterp.modules.inventory.entity.StockMovement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, String> {

    List<StockMovement> findByProductId(String productId);

    List<StockMovement> findByBusinessId(String businessId);

    @Query("SELECT m FROM StockMovement m WHERE m.businessId = :businessId ORDER BY m.createdAt DESC")
    List<StockMovement> findTop10ByBusinessIdOrderByCreatedAtDesc(@Param("businessId") String businessId,
            Pageable pageable);

    @Query(value = "SELECT * FROM stock_movements WHERE business_id = :businessId ORDER BY created_at DESC LIMIT 10", nativeQuery = true)
    List<StockMovement> findTop10ByBusinessIdOrderByCreatedAtDesc(@Param("businessId") String businessId);

    List<StockMovement> findByReferenceId(String referenceId);
}