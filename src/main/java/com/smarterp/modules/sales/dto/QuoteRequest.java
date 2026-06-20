package com.smarterp.modules.sales.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuoteRequest {
    private String customerName;
    private String customerDocument;
    private String sellerName;
    private String notes;
    private List<QuoteItemRequest> items;
}