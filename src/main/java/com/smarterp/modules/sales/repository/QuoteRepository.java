package com.smarterp.modules.sales.repository;

import com.smarterp.modules.sales.entity.Quote;
import com.smarterp.modules.sales.entity.QuoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, String> {

    List<Quote> findByBusinessIdOrderByCreatedAtDesc(String businessId);

    Optional<Quote> findByQuoteNumber(String quoteNumber);

    @Query("SELECT q FROM Quote q WHERE q.businessId = :businessId AND q.status = :status ORDER BY q.createdAt DESC")
    List<Quote> findByBusinessIdAndStatus(@Param("businessId") String businessId, @Param("status") QuoteStatus status);

    @Query("SELECT q FROM Quote q WHERE q.businessId = :businessId AND q.createdAt BETWEEN :start AND :end ORDER BY q.createdAt DESC")
    List<Quote> findByBusinessIdAndDateRange(@Param("businessId") String businessId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT q FROM Quote q WHERE q.businessId = :businessId AND " +
            "(LOWER(q.quoteNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(q.customerName) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Quote> searchQuotes(@Param("businessId") String businessId, @Param("search") String search);
}