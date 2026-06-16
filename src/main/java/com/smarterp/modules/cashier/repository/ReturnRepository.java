package com.smarterp.modules.cashier.repository;

import com.smarterp.modules.cashier.entity.ReturnOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReturnRepository extends JpaRepository<ReturnOrder, String> {
}