package com.smarterp.modules.cashier.repository;

import com.smarterp.modules.cashier.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SaleRepository extends JpaRepository<Sale, String> {
    List<Sale> findByCashRegisterId(String cashRegisterId);

    List<Sale> findByCashierId(String cashierId);

    Page<Sale> findByCashierIdAndCashRegisterId(String cashierId, String cashRegisterId, Pageable pageable);

    List<Sale> findByBusinessId(String businessId);
}