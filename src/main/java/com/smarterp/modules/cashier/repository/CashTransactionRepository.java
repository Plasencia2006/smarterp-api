package com.smarterp.modules.cashier.repository;

import com.smarterp.modules.cashier.entity.CashTransaction;
import com.smarterp.modules.cashier.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CashTransactionRepository extends JpaRepository<CashTransaction, String> {

    List<CashTransaction> findByRegisterIdOrderByCreatedAtDesc(String registerId);

    List<CashTransaction> findByBusinessIdAndRegisterIdOrderByCreatedAtDesc(String businessId, String registerId);

    List<CashTransaction> findByQuoteId(String quoteId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM CashTransaction t WHERE t.registerId = :registerId AND t.type = :type")
    BigDecimal sumAmountByRegisterIdAndType(@Param("registerId") String registerId,
            @Param("type") TransactionType type);
}