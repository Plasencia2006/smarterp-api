package com.smarterp.modules.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "product_sku")
    private String productSku;

    @Column(name = "business_id", nullable = false)
    private String businessId;

    @Column(nullable = false)
    private Integer currentStock;

    @Column(name = "min_stock")
    private Integer minStock;

    @Column(nullable = false)
    private String alertType; // LOW_STOCK, OUT_OF_STOCK

    @Column(nullable = false)
    private Boolean isAttended;

    @Column(length = 500)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "attended_at")
    private LocalDateTime attendedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        isAttended = false;
    }
}