package com.smarterp.modules.inventory.repository;

import com.smarterp.modules.inventory.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, String> {
    List<Stock> findByBusinessId(String businessId);

    Optional<Stock> findByProductIdAndBusinessId(String productId, String businessId);
}