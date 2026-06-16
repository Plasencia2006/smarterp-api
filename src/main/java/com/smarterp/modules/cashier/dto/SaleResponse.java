package com.smarterp.modules.cashier.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SaleResponse {
    private String id;
    private String customerName;
    private String cashierName;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal total;
    private String status;
    private List<SaleItemResponse> items;
    private List<PaymentResponse> payments;
    private LocalDateTime createdAt;
}