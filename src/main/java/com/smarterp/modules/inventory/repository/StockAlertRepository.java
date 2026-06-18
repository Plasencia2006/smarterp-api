package com.smarterp.modules.inventory.repository;

import com.smarterp.modules.inventory.entity.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockAlertRepository extends JpaRepository<StockAlert, String> {

    List<StockAlert> findByBusinessIdAndIsAttended(String businessId, Boolean isAttended);

    List<StockAlert> findByBusinessIdOrderByCreatedAtDesc(String businessId);

    Optional<StockAlert> findByProductIdAndBusinessIdAndIsAttended(
            String productId, String businessId, Boolean isAttended);

    @Query("SELECT sa FROM StockAlert sa WHERE sa.businessId = :businessId " +
            "AND sa.isAttended = false AND sa.alertType = 'OUT_OF_STOCK'")
    List<StockAlert> findOutOfStockAlerts(@Param("businessId") String businessId);

    @Query("SELECT sa FROM StockAlert sa WHERE sa.businessId = :businessId " +
            "AND sa.isAttended = false AND sa.alertType = 'LOW_STOCK'")
    List<StockAlert> findLowStockAlerts(@Param("businessId") String businessId);
}