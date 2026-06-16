package com.smarterp.modules.sales.repository;

import com.smarterp.modules.sales.entity.SalesCommission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SalesCommissionRepository extends JpaRepository<SalesCommission, String> {
    List<SalesCommission> findBySellerId(String sellerId);
}