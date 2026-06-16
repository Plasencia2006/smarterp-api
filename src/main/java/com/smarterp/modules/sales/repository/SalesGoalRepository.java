package com.smarterp.modules.sales.repository;

import com.smarterp.modules.sales.entity.SalesGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SalesGoalRepository extends JpaRepository<SalesGoal, String> {
    List<SalesGoal> findBySellerId(String sellerId);
}