package com.smarterp.modules.accounting.repository;

import com.smarterp.modules.accounting.entity.IncomeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IncomeCategoryRepository extends JpaRepository<IncomeCategory, String> {
    List<IncomeCategory> findByBusinessId(String businessId);
}