package com.smarterp.modules.cashier.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_withdrawals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashWithdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "business_id", nullable = false)
    private String businessId;

    @Column(name = "register_id", nullable = false)
    private String registerId;

    //  Datos del cajero que solicita
    @Column(name = "cashier_id", nullable = false)
    private String cashierId;

    @Column(name = "cashier_name")
    private String cashierName;

    //  Datos del supervisor que aprueba
    @Column(name = "supervisor_id")
    private String supervisorId;

    @Column(name = "supervisor_name")
    private String supervisorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WithdrawalStatus status;

    //  Monto y motivo
    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "reason", length = 500, nullable = false)
    private String reason;

    @Column(name = "destination")
    private String destination; // "Caja Fuerte", "Banco", etc.

    @Column(name = "reference_number")
    private String referenceNumber; // Número de referencia del retiro

    // ✅ Aprobación/Rechazo
    @Column(name = "approval_notes", length = 500)
    private String approvalNotes;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null)
            status = WithdrawalStatus.SOLICITADO;
        if (requestedAt == null)
            requestedAt = LocalDateTime.now();
        if (referenceNumber == null) {
            referenceNumber = "RET-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}