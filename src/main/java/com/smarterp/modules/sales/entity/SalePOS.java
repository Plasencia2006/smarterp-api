package com.smarterp.modules.sales.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pos_sales") // ✅ Nombre de tabla único
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalePOS {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "business_id", nullable = false)
    private String businessId;

    @Column(name = "sale_number", unique = true)
    private String saleNumber;

    @Column(name = "voucher_type")
    private String voucherType;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_document")
    private String customerDocument;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SaleStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "subtotal", precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "igv", precision = 10, scale = 2)
    private BigDecimal igv;

    @Column(name = "discount", precision = 10, scale = 2)
    private BigDecimal discount;

    @Column(name = "total", precision = 10, scale = 2, nullable = false)
    private BigDecimal total;

    @Column(name = "amount_paid", precision = 10, scale = 2)
    private BigDecimal amountPaid;

    @Column(name = "change_amount", precision = 10, scale = 2)
    private BigDecimal changeAmount;

    @Column(length = 500)
    private String notes;

    @Column(name = "seller_name")
    private String sellerName;

    @OneToMany(mappedBy = "salePOS", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<SalePOSItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null)
            status = SaleStatus.COMPLETED;
        if (saleNumber == null || saleNumber.isBlank()) {
            saleNumber = "V-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addItem(SalePOSItem item) {
        items.add(item);
        item.setSalePOS(this);
    }

    public void calculateTotals() {
        this.subtotal = items.stream()
                .map(SalePOSItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.igv = this.subtotal.multiply(new BigDecimal("0.18"));
        if (this.discount == null)
            this.discount = BigDecimal.ZERO;
        this.total = this.subtotal.add(this.igv).subtract(this.discount);
        if (this.amountPaid != null && this.amountPaid.compareTo(this.total) > 0) {
            this.changeAmount = this.amountPaid.subtract(this.total);
        }
    }
}