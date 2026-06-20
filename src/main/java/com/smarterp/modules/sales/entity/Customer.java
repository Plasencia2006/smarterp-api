package com.smarterp.modules.sales.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // ✅ business_id es solo un campo de texto, NO una foreign key
    @Column(name = "business_id", nullable = false)
    private String businessId; // Solo referencia, SIN @ManyToOne

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "document_type")
    private String documentType;

    @Column(name = "document_number")
    private String documentNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "address")
    private String address;

    @Column(name = "notes")
    private String notes;

    @Column(name = "is_frequent")
    private Boolean isFrequent;

    @Column(name = "total_purchases", precision = 10, scale = 2)
    private BigDecimal totalPurchases;

    @Column(name = "purchase_count")
    private Integer purchaseCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isFrequent == null)
            isFrequent = false;
        if (totalPurchases == null)
            totalPurchases = BigDecimal.ZERO;
        if (purchaseCount == null)
            purchaseCount = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}