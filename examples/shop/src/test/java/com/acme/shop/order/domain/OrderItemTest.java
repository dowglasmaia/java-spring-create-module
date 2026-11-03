package com.acme.shop.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrderItemTest {

    @Test
    void subtotalIsQuantityTimesPrice() {
        assertThat(new OrderItem("a", 3, new BigDecimal("2.50")).subtotal()).isEqualByComparingTo("7.50");
    }

    @Test
    void rejectsABlankSku() {
        assertThatThrownBy(() -> new OrderItem(" ", 1, BigDecimal.ONE)).isInstanceOf(InvalidOrderException.class).hasMessage("Item sku is required.");
    }

    @Test
    void rejectsAZeroQuantity() {
        assertThatThrownBy(() -> new OrderItem("a", 0, BigDecimal.ONE)).isInstanceOf(InvalidOrderException.class);
    }

    @Test
    void rejectsANegativePrice() {
        assertThatThrownBy(() -> new OrderItem("a", 1, new BigDecimal("-0.01"))).isInstanceOf(InvalidOrderException.class);
    }
}
