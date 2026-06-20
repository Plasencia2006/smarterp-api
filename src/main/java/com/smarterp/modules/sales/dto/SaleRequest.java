package com.smarterp.modules.sales.dto;

import com.smarterp.modules.sales.entity.PaymentMethod;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleRequest {
    private String voucherType;
    private String customerName;
    private String customerDocument;
    private PaymentMethod paymentMethod;
    private BigDecimal discount;
    private BigDecimal amountPaid;
    private String notes;
    private String sellerName;
    private List<SaleItemRequest> items;
}