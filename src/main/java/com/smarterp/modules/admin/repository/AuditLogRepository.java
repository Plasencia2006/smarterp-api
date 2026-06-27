package com.smarterp.modules.admin.repository;

import com.smarterp.modules.admin.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    // Por negocio
    Page<AuditLog> findByBusinessId(String businessId, Pageable pageable);

    // Por usuario
    Page<AuditLog> findByUserId(String userId, Pageable pageable);

    // Por acción
    Page<AuditLog> findByAction(String action, Pageable pageable);

    // Por entidad
    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);

    // Por negocio y fecha
    Page<AuditLog> findByBusinessIdAndCreatedAtBetween(
            String businessId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable);

    // Por negocio y usuario
    Page<AuditLog> findByBusinessIdAndUserId(
            String businessId,
            String userId,
            Pageable pageable);

    // Búsqueda avanzada
    @Query("SELECT a FROM AuditLog a WHERE a.businessId = :businessId " +
            "AND (:action IS NULL OR a.action = :action) " +
            "AND (:entityType IS NULL OR a.entityType = :entityType) " +
            "AND (:userId IS NULL OR a.userId = :userId) " +
            "AND (a.createdAt BETWEEN :startDate AND :endDate) " +
            "ORDER BY a.createdAt DESC")
    Page<AuditLog> findAdvanced(
            @Param("businessId") String businessId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("userId") String userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    // Conteo por acción
    @Query("SELECT a.action, COUNT(a) FROM AuditLog a " +
            "WHERE a.businessId = :businessId " +
            "AND a.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY a.action ORDER BY COUNT(a) DESC")
    List<Object[]> countByAction(
            @Param("businessId") String businessId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Últimas actividades
    List<AuditLog> findTop10ByBusinessIdOrderByCreatedAtDesc(String businessId);

    // Actividades de un usuario específico
    List<AuditLog> findByUserIdOrderByCreatedAtDesc(String userId);
}