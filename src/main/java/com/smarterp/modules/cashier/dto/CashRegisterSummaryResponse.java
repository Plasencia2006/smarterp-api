package com.smarterp.modules.cashier.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CashRegisterSummaryResponse {
    private String id;
    private String cashierName;
    private BigDecimal initialAmount;
    private BigDecimal finalAmount;
    private BigDecimal totalSales;
    private BigDecimal totalReturns;
    private Integer salesCount;
    private String status;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
}