package com.acme.shop.order.infrastructure.persistence;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;

/** Storage shape of an order line. Infra class: never leaves the persistence package. */
@Embeddable
public class OrderItemEmbeddable {

    private String sku;
    private int quantity;
    private BigDecimal unitPrice;

    protected OrderItemEmbeddable() {
        // JPA
    }

    public OrderItemEmbeddable(String sku, int quantity, BigDecimal unitPrice) {
        this.sku = sku;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public String getSku() {
        return sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
}
