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

        Optional<Quote> findByQuoteNumber(String quoteNumber);

        List<Quote> findByBusinessIdOrderByCreatedAtDesc(String businessId);

        List<Quote> findByBusinessIdAndStatus(String businessId, QuoteStatus status);

        // ✅ NUEVO: Cotizaciones pendientes (para el cajero)
        @Query("SELECT q FROM Quote q WHERE q.businessId = :businessId " +
                        "AND q.status = 'PENDIENTE' " +
                        "ORDER BY q.createdAt DESC")
        List<Quote> findPendingQuotesByBusinessId(@Param("businessId") String businessId);

        // ✅ NUEVO: Cotizaciones pendientes con bloqueo activo
        @Query("SELECT q FROM Quote q WHERE q.businessId = :businessId " +
                        "AND q.status = 'PENDIENTE' " +
                        "AND q.isBlocked = true " +
                        "AND q.blockedUntil > CURRENT_TIMESTAMP " +
                        "ORDER BY q.blockedUntil ASC")
        List<Quote> findActiveBlockedQuotes(@Param("businessId") String businessId);

        // NUEVOS MÉTODOS PARA GESTIÓN DE FACTURAS

        /**
         * Buscar facturas por negocio y rango de fechas
         */
        @Query("SELECT q FROM Quote q WHERE q.businessId = :businessId " +
                        "AND q.status = 'FACTURADA' " +
                        "AND q.paidAt BETWEEN :start AND :end " +
                        "ORDER BY q.paidAt DESC")
        List<Quote> findInvoicesByBusinessIdAndDateRange(
                        @Param("businessId") String businessId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        /**
         * Buscar facturas por número de factura
         */
        Optional<Quote> findByInvoiceNumberAndBusinessId(String invoiceNumber, String businessId);

        /**
         * Buscar facturas por cliente (nombre contiene)
         */
        @Query("SELECT q FROM Quote q WHERE q.businessId = :businessId " +
                        "AND q.status = 'FACTURADA' " +
                        "AND LOWER(q.customerName) LIKE LOWER(CONCAT('%', :customerName, '%')) " +
                        "ORDER BY q.paidAt DESC")
        List<Quote> findInvoicesByCustomerName(
                        @Param("businessId") String businessId,
                        @Param("customerName") String customerName);

        /**
         * Buscar facturas por método de pago
         */
        List<Quote> findByBusinessIdAndStatusAndPaymentMethod(
                        String businessId, QuoteStatus status, String paymentMethod);

        /**
         * Facturas del día actual
         */
        @Query("SELECT q FROM Quote q WHERE q.businessId = :businessId " +
                        "AND q.status = 'FACTURADA' " +
                        "AND q.paidAt >= :startOfDay " +
                        "ORDER BY q.paidAt DESC")
        List<Quote> findTodayInvoices(
                        @Param("businessId") String businessId,
                        @Param("startOfDay") LocalDateTime startOfDay);

        /**
         * Total vendido en rango de fechas
         */
        @Query("SELECT COALESCE(SUM(q.total), 0) FROM Quote q " +
                        "WHERE q.businessId = :businessId " +
                        "AND q.status = 'FACTURADA' " +
                        "AND q.paidAt BETWEEN :start AND :end")
        java.math.BigDecimal sumTotalSalesByDateRange(
                        @Param("businessId") String businessId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        /**
         * Total vendido por método de pago
         */
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

        /**
         * Contar facturas en rango de fechas
         */
        @Query("SELECT COUNT(q) FROM Quote q " +
                        "WHERE q.businessId = :businessId " +
                        "AND q.status = 'FACTURADA' " +
                        "AND q.paidAt BETWEEN :start AND :end")
        Long countInvoicesByDateRange(
                        @Param("businessId") String businessId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

}