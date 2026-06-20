package com.smarterp.modules.sales.repository;

import com.smarterp.modules.sales.entity.SalePOS;
import com.smarterp.modules.sales.entity.SaleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SalePOSRepository extends JpaRepository<SalePOS, String> {

        List<SalePOS> findByBusinessIdOrderByCreatedAtDesc(String businessId);

        @Query("SELECT s FROM SalePOS s WHERE s.businessId = :businessId AND s.status = :status ORDER BY s.createdAt DESC")
        List<SalePOS> findByBusinessIdAndStatus(@Param("businessId") String businessId,
                        @Param("status") SaleStatus status);

        @Query("SELECT s FROM SalePOS s WHERE s.businessId = :businessId AND s.createdAt BETWEEN :start AND :end ORDER BY s.createdAt DESC")
        List<SalePOS> findByBusinessIdAndDateRange(@Param("businessId") String businessId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("SELECT s FROM SalePOS s WHERE s.businessId = :businessId AND " +
                        "(LOWER(s.saleNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(s.customerName) LIKE LOWER(CONCAT('%', :search, '%')))")
        List<SalePOS> searchSales(@Param("businessId") String businessId, @Param("search") String search);
}