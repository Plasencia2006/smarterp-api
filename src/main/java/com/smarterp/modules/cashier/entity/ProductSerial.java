package com.smarterp.modules.cashier.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_serials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSerial {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "business_id", nullable = false)
    private String businessId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "serial_number", nullable = false, unique = true)
    private String serialNumber;

    @Column(name = "imei")
    private String imei;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SerialStatus status;

    @Column(name = "quote_item_id")
    private String quoteItemId;

    @Column(name = "quote_id")
    private String quoteId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "warranty_start")
    private LocalDateTime warrantyStart;

    @Column(name = "warranty_end")
    private LocalDateTime warrantyEnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null)
            status = SerialStatus.DISPONIBLE;
    }
}