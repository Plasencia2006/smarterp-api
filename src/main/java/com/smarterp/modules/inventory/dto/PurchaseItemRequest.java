package com.smarterp.modules.inventory.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseItemRequest {
    private String productId;
    private String productName;
    private String productSku;
    private Integer quantity;
    private BigDecimal unitCost;
}