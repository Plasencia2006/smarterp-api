package com.smarterp.modules.accounting.repository;

import com.smarterp.modules.accounting.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, String> {
    List<ExpenseCategory> findByBusinessId(String businessId);
}