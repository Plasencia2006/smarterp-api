package com.smarterp.modules.cashier.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashRegisterSummaryResponse {
    private String registerId;
    private String userName;
    private LocalDateTime openingTime;
    private LocalDateTime closingTime;
    private String status;
    private BigDecimal initialCash;
    private BigDecimal finalCash;
    private BigDecimal expectedCash;
    private BigDecimal cashDifference;
    private BigDecimal totalIngresos;
    private BigDecimal totalEgresos;
    private Long totalVentas;
    private List<TransactionSummary> transactions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TransactionSummary {
        private String type;
        private BigDecimal amount;
        private String description;
        private String paymentMethod;
        private LocalDateTime createdAt;
    }
}