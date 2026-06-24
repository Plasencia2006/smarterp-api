package com.smarterp.modules.cashier.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_audits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "business_id", nullable = false)
    private String businessId;

    @Column(name = "register_id", nullable = false)
    private String registerId;

    // ✅ Datos del cajero auditado
    @Column(name = "cashier_id", nullable = false)
    private String cashierId;

    @Column(name = "cashier_name")
    private String cashierName;

    // ✅ Datos del supervisor que realiza el arqueo
    @Column(name = "supervisor_id")
    private String supervisorId;

    @Column(name = "supervisor_name")
    private String supervisorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AuditStatus status;

    // ✅ Montos del arqueo
    @Column(name = "expected_cash", precision = 10, scale = 2)
    private BigDecimal expectedCash; // Lo que el sistema dice que debe haber

    @Column(name = "counted_cash", precision = 10, scale = 2)
    private BigDecimal countedCash; // Lo que el supervisor contó físicamente

    @Column(name = "difference", precision = 10, scale = 2)
    private BigDecimal difference; // countedCash - expectedCash

    // ✅ Detalles del conteo
    @Column(name = "bills_200", precision = 10, scale = 2)
    private BigDecimal bills200;

    @Column(name = "bills_100", precision = 10, scale = 2)
    private BigDecimal bills100;

    @Column(name = "bills_50", precision = 10, scale = 2)
    private BigDecimal bills50;

    @Column(name = "bills_20", precision = 10, scale = 2)
    private BigDecimal bills20;

    @Column(name = "bills_10", precision = 10, scale = 2)
    private BigDecimal bills10;

    @Column(name = "coins", precision = 10, scale = 2)
    private BigDecimal coins;

    @Column(name = "vouchers", precision = 10, scale = 2)
    private BigDecimal vouchers; // Vales, cupones, etc.

    // ✅ Metadata
    @Column(name = "audit_type", length = 20)
    private String auditType; // PARCIAL, CIERRE, SORPRESA

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "audit_number")
    private String auditNumber;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

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
            status = AuditStatus.PENDIENTE;
        if (auditNumber == null) {
            auditNumber = "AUD-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Calcular diferencia automáticamente
     */
    public void calculateDifference() {
        if (this.expectedCash != null && this.countedCash != null) {
            this.difference = this.countedCash.subtract(this.expectedCash);
        }
    }

    /**
     * Calcular total contado desde los detalles
     */
    public BigDecimal calculateCountedFromDetails() {
        BigDecimal total = BigDecimal.ZERO;
        if (bills200 != null)
            total = total.add(bills200);
        if (bills100 != null)
            total = total.add(bills100);
        if (bills50 != null)
            total = total.add(bills50);
        if (bills20 != null)
            total = total.add(bills20);
        if (bills10 != null)
            total = total.add(bills10);
        if (coins != null)
            total = total.add(coins);
        if (vouchers != null)
            total = total.add(vouchers);
        return total;
    }
}