package com.smarterp.modules.sales.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "pos_sale_items") // ✅ Nombre de tabla único
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalePOSItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_pos_id", nullable = false) // ✅ FK única
    @JsonBackReference
    private SalePOS salePOS;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_sku")
    private String productSku;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "cost_price", precision = 10, scale = 2)
    private BigDecimal costPrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal discount;

    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal;

    @PrePersist
    protected void onCreate() {
        if (discount == null)
            discount = BigDecimal.ZERO;
        calculateSubtotal();
    }

    public void calculateSubtotal() {
        BigDecimal base = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.subtotal = base.subtract(discount);
    }
}