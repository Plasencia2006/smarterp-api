package com.smarterp.modules.accounting.repository;

import com.smarterp.modules.accounting.entity.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IncomeRepository extends JpaRepository<Income, String> {
    List<Income> findByBusinessId(String businessId);
}