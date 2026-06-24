package com.smarterp.modules.cashier.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashAuditResponse {
    private String id;
    private String auditNumber;
    private String registerId;
    private String cashierName;
    private String supervisorName;
    private String status;
    private String auditType;

    // Montos
    private BigDecimal expectedCash;
    private BigDecimal countedCash;
    private BigDecimal difference;
    private Boolean isConcordant;

    // Detalles del conteo
    private BigDecimal bills200;
    private BigDecimal bills100;
    private BigDecimal bills50;
    private BigDecimal bills20;
    private BigDecimal bills10;
    private BigDecimal coins;
    private BigDecimal vouchers;

    // Metadata
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String notes;
}