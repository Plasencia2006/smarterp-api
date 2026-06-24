package com.smarterp.modules.cashier.repository;

import com.smarterp.modules.cashier.entity.CashWithdrawal;
import com.smarterp.modules.cashier.entity.WithdrawalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CashWithdrawalRepository extends JpaRepository<CashWithdrawal, String> {

    List<CashWithdrawal> findByRegisterIdOrderByCreatedAtDesc(String registerId);

    List<CashWithdrawal> findByBusinessIdOrderByCreatedAtDesc(String businessId);

    List<CashWithdrawal> findByRegisterIdAndStatus(String registerId, WithdrawalStatus status);

    @Query("SELECT w FROM CashWithdrawal w WHERE w.registerId = :registerId " +
            "AND w.requestedAt BETWEEN :start AND :end " +
            "ORDER BY w.requestedAt DESC")
    List<CashWithdrawal> findByRegisterIdAndDateRange(
            @Param("registerId") String registerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM CashWithdrawal w " +
            "WHERE w.registerId = :registerId " +
            "AND w.status IN ('APROBADO', 'COMPLETADO')")
    BigDecimal sumApprovedWithdrawalsByRegisterId(@Param("registerId") String registerId);

    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM CashWithdrawal w " +
            "WHERE w.registerId = :registerId " +
            "AND w.status IN ('APROBADO', 'COMPLETADO') " +
            "AND w.completedAt BETWEEN :start AND :end")
    BigDecimal sumWithdrawalsByRegisterIdAndDateRange(
            @Param("registerId") String registerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}