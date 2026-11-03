package com.acme.shop.order.domain;

import java.math.BigDecimal;

/** Value object: it cannot exist in an invalid state. */
public record OrderItem(String sku, int quantity, BigDecimal unitPrice) {

    public OrderItem {
        if (sku == null || sku.isBlank()) {
            throw new InvalidOrderException("Item sku is required.");
        }
        if (quantity <= 0) {
            throw new InvalidOrderException("Item quantity must be greater than zero.");
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new InvalidOrderException("Item price must not be negative.");
        }
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
