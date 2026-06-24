package com.smarterp.modules.cashier.repository;

import com.smarterp.modules.cashier.entity.AuditStatus;
import com.smarterp.modules.cashier.entity.CashAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CashAuditRepository extends JpaRepository<CashAudit, String> {

    List<CashAudit> findByRegisterIdOrderByCreatedAtDesc(String registerId);

    List<CashAudit> findByBusinessIdOrderByCreatedAtDesc(String businessId);

    List<CashAudit> findByRegisterIdAndStatus(String registerId, AuditStatus status);

    Optional<CashAudit> findTopByRegisterIdAndStatusOrderByStartedAtDesc(
            String registerId, AuditStatus status);

    @Query("SELECT a FROM CashAudit a WHERE a.registerId = :registerId " +
            "AND a.startedAt BETWEEN :start AND :end " +
            "ORDER BY a.startedAt DESC")
    List<CashAudit> findByRegisterIdAndDateRange(
            @Param("registerId") String registerId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(a) FROM CashAudit a WHERE a.registerId = :registerId " +
            "AND a.status = 'DISCORDANTE'")
    Long countDiscrepanciesByRegisterId(@Param("registerId") String registerId);

    @Query("SELECT COALESCE(SUM(ABS(a.difference)), 0) FROM CashAudit a " +
            "WHERE a.registerId = :registerId AND a.status = 'DISCORDANTE'")
    java.math.BigDecimal sumAbsoluteDifferencesByRegisterId(@Param("registerId") String registerId);
}