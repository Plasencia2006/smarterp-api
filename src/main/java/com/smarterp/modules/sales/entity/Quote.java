package com.smarterp.modules.sales.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quotes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "business_id", nullable = false)
    private String businessId;

    @Column(name = "seller_id")
    private String sellerId;

    @Column(name = "seller_name")
    private String sellerName;

    @Column(name = "quote_number", unique = true, nullable = false)
    private String quoteNumber;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_document")
    private String customerDocument;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private QuoteStatus status;

    @Column(name = "subtotal", precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "tax", precision = 10, scale = 2)
    private BigDecimal tax;

    @Column(name = "igv", precision = 10, scale = 2)
    private BigDecimal igv;

    @Column(name = "total", precision = 10, scale = 2, nullable = false)
    private BigDecimal total;

    @Column(length = 500)
    private String notes;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // ✅ CAMPOS PARA BLOQUEO TEMPORAL
    @Column(name = "blocked_until")
    private LocalDateTime blockedUntil;

    @Column(name = "is_blocked")
    private Boolean isBlocked;

    @Column(name = "block_duration_minutes")
    private Integer blockDurationMinutes;

    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<QuoteItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null)
            status = QuoteStatus.PENDIENTE;
        if (quoteNumber == null || quoteNumber.isBlank()) {
            quoteNumber = "Q-" + System.currentTimeMillis();
        }
        if (tax == null)
            tax = BigDecimal.ZERO;
        if (subtotal == null)
            subtotal = BigDecimal.ZERO;
        if (igv == null)
            igv = BigDecimal.ZERO;
        if (total == null)
            total = BigDecimal.ZERO;
        if (isBlocked == null)
            isBlocked = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addItem(QuoteItem item) {
        items.add(item);
        item.setQuote(this);
    }

    public void calculateTotals() {
        if (this.items == null) {
            this.items = new ArrayList<>();
        }

        this.subtotal = this.items.stream()
                .map(item -> {
                    if (item.getSubtotal() == null) {
                        BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                        Integer qty = item.getQuantity() != null ? item.getQuantity() : 1;
                        return price.multiply(BigDecimal.valueOf(qty));
                    }
                    return item.getSubtotal();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (this.subtotal == null) {
            this.subtotal = BigDecimal.ZERO;
        }

        this.igv = this.subtotal.multiply(new BigDecimal("0.18"));
        this.tax = this.igv;

        if (this.igv == null) {
            this.igv = BigDecimal.ZERO;
            this.tax = BigDecimal.ZERO;
        }

        this.total = this.subtotal.add(this.igv);
    }

    // ✅ MÉTODOS PARA BLOQUEO
    public boolean isExpired() {
        if (this.blockedUntil == null || this.isBlocked == null || !this.isBlocked) {
            return false;
        }
        return LocalDateTime.now().isAfter(this.blockedUntil);
    }

    public long getRemainingMinutes() {
        if (this.blockedUntil == null) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), this.blockedUntil).toMinutes();
    }

    public void activateBlock(int minutes) {
        this.isBlocked = true;
        this.blockDurationMinutes = minutes;
        this.blockedUntil = LocalDateTime.now().plusMinutes(minutes);
        System.out.println(" Bloqueo activado: " + minutes + " minutos (hasta " + this.blockedUntil + ")");
    }

    public void releaseBlock() {
        this.isBlocked = false;
        this.blockedUntil = null;
        this.blockDurationMinutes = null;
    }
}