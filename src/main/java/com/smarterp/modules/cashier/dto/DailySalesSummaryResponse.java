package com.smarterp.modules.cashier.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailySalesSummaryResponse {
    private String date;
    private Long totalInvoices;
    private BigDecimal totalSales;
    private BigDecimal averageTicket;
    private List<SalesByPaymentMethodResponse> salesByPaymentMethod;
    private List<HourlySalesResponse> hourlySales;
    private String topProduct;
    private Long topProductQuantity;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HourlySalesResponse {
        private Integer hour;
        private Long invoiceCount;
        private BigDecimal totalAmount;
    }
}