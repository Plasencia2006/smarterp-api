package com.smarterp.modules.inventory.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderRequest {
    private String supplierId;
    private String supplierName;
    private String notes;
    private List<PurchaseItemRequest> items;
}