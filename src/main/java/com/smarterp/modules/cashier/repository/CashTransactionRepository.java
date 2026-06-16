package com.smarterp.modules.cashier.repository;

import com.smarterp.modules.cashier.entity.CashTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CashTransactionRepository extends JpaRepository<CashTransaction, String> {
    List<CashTransaction> findByCashRegisterId(String cashRegisterId);
}