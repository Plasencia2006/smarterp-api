package com.smarterp.modules.sales.repository;

import com.smarterp.modules.sales.entity.Quote;
import com.smarterp.modules.sales.entity.QuoteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, String> {

        // ============================================
        // 📋 MÉTODOS BÁSICOS
        // ============================================

        Optional<Quote> findByQuoteNumber(String quoteNumber);

        List<Quote> findByBusinessIdOrderByCreatedAtDesc(String businessId);

        List<Quote> findByBusinessIdAndStatus(String businessId, QuoteStatus status);

        // ============================================
        // 👤 MÉTODOS POR VENDEDOR (para vendedor individual)
        // ============================================

        List<Quote> findByBusinessIdAndSellerIdOrderByCreatedAtDesc(String businessId, String sellerId);

        // ============================================
        // ⏳ COTIZACIONES PENDIENTES
        // ============================================

        @Query("SELECT q FROM Quote q WHERE q.businessId = :businessId " +
                        "AND q.status = 'PENDIENTE' " +
                        "ORDER BY q.createdAt DESC")
        List<Quote> findPendingQuotesByBusinessId(@Param("businessId") String businessId);

        @Query("SELECT q FROM Quote q WHERE q.businessId = :businessId " +
                        "AND q.sellerId = :sellerId " +
                        "AND q.status = 'PENDIENTE' " +
                        "ORDER BY q.createdAt DESC")
        List<Quote> findPendingQuotesByBusinessIdAndSellerId(
                        @Param("businessId") String businessId,
                        @Param("sellerId") String sellerId);

        // ============================================
        // 🔒 COTIZACIONES BLOQUEADAS
        // ============================================

        @Query("SELECT q FROM Quote q WHERE q.businessId = :businessId " +
                        "AND q.status = 'PENDIENTE' " +
                        "AND q.isBlocked = true " +
                        "AND q.blockedUntil > CURRENT_TIMESTAMP " +
                        "ORDER BY q.blockedUntil ASC")
        List<Quote> findActiveBlockedQuotes(@Param("businessId") String businessId);

        @Query("SELECT q FROM Quote q WHERE q.businessId = :businessId " +
                        "AND q.sellerId = :sellerId " +
                        "AND q.status = 'PENDIENTE' " +
                        "AND q.isBlocked = true " +
                        "AND q.blockedUntil > CURRENT_TIMESTAMP " +
                        "ORDER BY q.blockedUntil ASC")
        List<Quote> findActiveBlockedQuotesBySeller(
                        @Param("businessId") String businessId,
                        @Param("sellerId") String sellerId);

        // ============================================
        // 🧾 GESTIÓN DE FACTURAS
        // ============================================

        @Query("SELECT q FROM Quote q WHERE q.businessId = :businessId " +
                        "AND q.status = 'FACTURADA' " +
                        "AND q.paidAt BETWEEN :start AND :end " +
                        "ORDER BY q.paidAt DESC")
        List<Quote> findInvoicesByBusinessIdAndDateRange(
                        @Param("businessId") String businessId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        Optional<Quote> findByInvoiceNumberAndBusinessId(String invoiceNumber, String businessId);

        @Query("SELECT q FROM Quote q WHERE q.businessId = :businessId " +
                        "AND q.status = 'FACTURADA' " +
                        "AND LOWER(q.customerName) LIKE LOWER(CONCAT('%', :customerName, '%')) " +
                        "ORDER BY q.paidAt DESC")
        List<Quote> findInvoicesByCustomerName(
                        @Param("businessId") String businessId,
                        @Param("customerName") String customerName);

        List<Quote> findByBusinessIdAndStatusAndPaymentMethod(
                        String businessId, QuoteStatus status, String paymentMethod);

        @Query("SELECT q FROM Quote q WHERE q.businessId = :businessId " +
                        "AND q.status = 'FACTURADA' " +
                        "AND q.paidAt >= :startOfDay " +
                        "ORDER BY q.paidAt DESC")
        List<Quote> findTodayInvoices(
                        @Param("businessId") String businessId,
                        @Param("startOfDay") LocalDateTime startOfDay);

        // ============================================
        // 💰 MÉTODOS DE CÁLCULO (SUMAS)
        // ============================================

        @Query("SELECT COALESCE(SUM(q.total), 0) FROM Quote q " +
                        "WHERE q.businessId = :businessId " +
                        "AND q.status = 'FACTURADA' " +
                        "AND q.paidAt BETWEEN :start AND :end")
        java.math.BigDecimal sumTotalSalesByDateRange(
                        @Param("businessId") String businessId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("SELECT COALESCE(SUM(q.total), 0) FROM Quote q " +
                        "WHERE q.businessId = :businessId " +
                        "AND q.status = 'FACTURADA' " +
                        "AND q.paymentMethod = :paymentMethod " +
                        "AND q.paidAt BETWEEN :start AND :end")
        java.math.BigDecimal sumTotalSalesByPaymentMethod(
                        @Param("businessId") String businessId,
                        @Param("paymentMethod") String paymentMethod,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("SELECT COUNT(q) FROM Quote q " +
                        "WHERE q.businessId = :businessId " +
                        "AND q.status = 'FACTURADA' " +
                        "AND q.paidAt BETWEEN :start AND :end")
        Long countInvoicesByDateRange(
                        @Param("businessId") String businessId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        // ============================================
        // 👑 MÉTODOS PARA ADMINISTRADOR (con paginación)
        // ============================================

        /**
         * ✅ Todas las cotizaciones del negocio con paginación
         */
        @Query("SELECT q FROM Quote q WHERE q.businessId = :businessId ORDER BY q.createdAt DESC")
        Page<Quote> findByBusinessId(
                        @Param("businessId") String businessId,
                        Pageable pageable);

        /**
         * ✅ Cotizaciones por vendedor con paginación
         */
        @Query("SELECT q FROM Quote q WHERE q.businessId = :businessId AND q.sellerId = :sellerId ORDER BY q.createdAt DESC")
        Page<Quote> findByBusinessIdAndSellerId(
                        @Param("businessId") String businessId,
                        @Param("sellerId") String sellerId,
                        Pageable pageable);

        /**
         * ✅ Cotizaciones por estado con paginación (sobrecarga)
         */
        @Query("SELECT q FROM Quote q WHERE q.businessId = :businessId AND q.status = :status ORDER BY q.createdAt DESC")
        Page<Quote> findByBusinessIdAndStatus(
                        @Param("businessId") String businessId,
                        @Param("status") QuoteStatus status,
                        Pageable pageable);

        /**
         * ✅ Cotizaciones por vendedor y estado con paginación
         */
        @Query("SELECT q FROM Quote q WHERE q.businessId = :businessId AND q.sellerId = :sellerId AND q.status = :status ORDER BY q.createdAt DESC")
        Page<Quote> findByBusinessIdAndSellerIdAndStatus(
                        @Param("businessId") String businessId,
                        @Param("sellerId") String sellerId,
                        @Param("status") QuoteStatus status,
                        Pageable pageable);

        /**
         * ✅ Cotizaciones por rango de fechas de pago
         */
        @Query("SELECT q FROM Quote q WHERE q.businessId = :businessId AND q.paidAt BETWEEN :start AND :end ORDER BY q.paidAt DESC")
        List<Quote> findByBusinessIdAndPaidAtBetween(
                        @Param("businessId") String businessId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        /**
         * ✅ Cotizaciones por estados y rango de fechas
         */
        @Query("SELECT q FROM Quote q WHERE q.businessId = :businessId AND q.status IN :statuses AND q.paidAt BETWEEN :start AND :end ORDER BY q.paidAt DESC")
        List<Quote> findByBusinessIdAndStatusInAndPaidAtBetween(
                        @Param("businessId") String businessId,
                        @Param("statuses") List<QuoteStatus> statuses,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        /**
         * ✅ Cotizaciones por vendedor y rango de fechas
         */
        @Query("SELECT q FROM Quote q WHERE q.businessId = :businessId AND q.sellerId = :sellerId AND q.paidAt BETWEEN :start AND :end ORDER BY q.paidAt DESC")
        List<Quote> findByBusinessIdAndSellerIdAndPaidAtBetween(
                        @Param("businessId") String businessId,
                        @Param("sellerId") String sellerId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        /**
         * ✅ Contar cotizaciones por vendedor
         */
        @Query("SELECT COUNT(q) FROM Quote q WHERE q.businessId = :businessId AND q.sellerId = :sellerId")
        Long countByBusinessIdAndSellerId(
                        @Param("businessId") String businessId,
                        @Param("sellerId") String sellerId);

        /**
         * ✅ Contar cotizaciones por vendedor y estado
         */
        @Query("SELECT COUNT(q) FROM Quote q WHERE q.businessId = :businessId AND q.sellerId = :sellerId AND q.status = :status")
        Long countByBusinessIdAndSellerIdAndStatus(
                        @Param("businessId") String businessId,
                        @Param("sellerId") String sellerId,
                        @Param("status") QuoteStatus status);

        /**
         * ✅ Sumar ventas por vendedor en rango de fechas
         */
        @Query("SELECT COALESCE(SUM(q.total), 0) FROM Quote q " +
                        "WHERE q.businessId = :businessId " +
                        "AND q.sellerId = :sellerId " +
                        "AND q.status = 'FACTURADA' " +
                        "AND q.paidAt BETWEEN :start AND :end")
        java.math.BigDecimal sumTotalSalesBySellerAndDateRange(
                        @Param("businessId") String businessId,
                        @Param("sellerId") String sellerId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);
}