package com.smarterp.modules.cashier.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashFlowSummaryResponse {
    private String registerId;
    private String registerStatus;

    // Flujo de efectivo
    private BigDecimal initialCash;
    private BigDecimal totalIngresos;
    private BigDecimal totalEgresos;
    private BigDecimal totalWithdrawals; // Retiros aprobados
    private BigDecimal currentCash; // Efectivo actual en caja

    // Estadísticas de arqueos
    private Long totalAudits;
    private Long concordantAudits;
    private Long discrepantAudits;
    private BigDecimal totalDiscrepancy;

    // Estadísticas de retiros
    private Long totalWithdrawalsCount;
    private BigDecimal totalWithdrawnAmount;
    private Long pendingWithdrawals;

    // Alertas
    private List<String> alerts;

    // Últimos movimientos
    private List<RecentMovement> recentMovements;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentMovement {
        private String type; // VENTA, RETIRO, ARQUEO, EGRESO
        private String description;
        private BigDecimal amount;
        private String dateTime;
        private String status;
    }
}