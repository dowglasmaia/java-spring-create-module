package com.acme.shop.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Domain test: plain JUnit, no Spring, no mocks. Every invariant and transition, valid and invalid. */
class OrderTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final UUID CUSTOMER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static Order newOrder() {
        return Order.create(CUSTOMER, List.of(new OrderItem("sku-1", 2, new BigDecimal("10.50"))), NOW);
    }

    @Test
    void aNewOrderStartsAsNewWithAnIdentity() {
        Order order = newOrder();

        assertThat(order.id()).isNotNull();
        assertThat(order.status()).isEqualTo(OrderStatus.NEW);
        assertThat(order.createdAt()).isEqualTo(NOW);
    }

    @Test
    void anOrderNeedsAtLeastOneItem() {
        assertThatThrownBy(() -> Order.create(CUSTOMER, List.of(), NOW))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessage("An order needs at least one item.");
    }

    @Test
    void theTotalIsTheSumOfTheSubtotals() {
        Order order = Order.create(CUSTOMER, List.of(
                new OrderItem("a", 2, new BigDecimal("10.50")),
                new OrderItem("b", 1, new BigDecimal("4.00"))), NOW);

        assertThat(order.total()).isEqualByComparingTo("25.00");
    }

    @Test
    void aNewOrderCanBePaid() {
        Order order = newOrder();

        order.pay();

        assertThat(order.status()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void aPaidOrderCannotBeCancelledNorPaidAgain() {
        Order order = newOrder();
        order.pay();

        assertThatThrownBy(order::cancel).isInstanceOf(InvalidOrderException.class).hasMessage("Only a new order can be cancelled.");
        assertThatThrownBy(order::pay).isInstanceOf(InvalidOrderException.class).hasMessage("Only a new order can be paid.");
    }

    @Test
    void aNewOrderCanBeCancelledButNotPaidAfterwards() {
        Order order = newOrder();

        order.cancel();

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThatThrownBy(order::pay).isInstanceOf(InvalidOrderException.class);
    }

    @Test
    void restoreRebuildsStoredStateWithoutApplyingCreationRules() {
        Order order = Order.restore(UUID.randomUUID(), CUSTOMER, List.of(new OrderItem("a", 1, BigDecimal.ONE)), NOW, OrderStatus.PAID);

        assertThat(order.status()).isEqualTo(OrderStatus.PAID);
    }
}
