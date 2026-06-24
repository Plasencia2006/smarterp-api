package com.smarterp.modules.cashier.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DirectSaleRequest {
    private String customerName;
    private String customerDocument;
    private String paymentMethod;
    private BigDecimal amountPaid;
    private List<DirectSaleItemRequest> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DirectSaleItemRequest {
        private String productId;
        private String productName;
        private String productSku;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}