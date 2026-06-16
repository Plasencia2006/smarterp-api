package com.smarterp.modules.inventory.repository;

import com.smarterp.modules.inventory.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, String> {
    List<StockMovement> findByStockId(String stockId);
}