package com.smarterp.modules.cashier.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_registers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashRegister {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "business_id", nullable = false)
    private String businessId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "user_name")
    private String userName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CashRegisterStatus status;

    @Column(name = "opening_time")
    private LocalDateTime openingTime;

    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    // ✅ NUEVO: Campo opened_by (quién abrió la caja)
    @Column(name = "opened_by")
    private String openedBy;

    @Column(name = "closing_time")
    private LocalDateTime closingTime;

    @Column(name = "initial_amount", precision = 10, scale = 2)
    private BigDecimal initialAmount;

    @Column(name = "final_cash", precision = 10, scale = 2)
    private BigDecimal finalCash;

    @Column(name = "expected_cash", precision = 10, scale = 2)
    private BigDecimal expectedCash;

    @Column(name = "cash_difference", precision = 10, scale = 2)
    private BigDecimal cashDifference;

    @Column(name = "opening_notes")
    private String openingNotes;

    @Column(name = "closing_notes")
    private String closingNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null)
            status = CashRegisterStatus.CERRADO;

        // Sincronizar opened_at con opening_time
        if (openedAt == null && openingTime != null) {
            openedAt = openingTime;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}