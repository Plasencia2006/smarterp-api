package com.smarterp.modules.cashier.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashWithdrawalResponse {
    private String id;
    private String referenceNumber;
    private String registerId;
    private String cashierName;
    private String supervisorName;
    private String status;

    private BigDecimal amount;
    private String reason;
    private String destination;

    private String approvalNotes;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime completedAt;
}