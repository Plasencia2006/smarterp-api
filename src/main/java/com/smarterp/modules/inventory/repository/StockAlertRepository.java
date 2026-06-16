package com.smarterp.modules.inventory.repository;

import com.smarterp.modules.inventory.entity.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StockAlertRepository extends JpaRepository<StockAlert, String> {
    List<StockAlert> findByIsAttendedFalse();
}