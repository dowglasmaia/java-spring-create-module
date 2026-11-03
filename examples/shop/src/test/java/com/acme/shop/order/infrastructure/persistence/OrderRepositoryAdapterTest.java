package com.acme.shop.order.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.shop.order.domain.Order;
import com.acme.shop.order.domain.OrderItem;
import com.acme.shop.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/** Adapter test: the round trip Domain -> Entity -> database -> Entity -> Domain. */
@DataJpaTest
@Import(OrderRepositoryAdapter.class)
class OrderRepositoryAdapterTest {

    @Autowired
    private OrderRepositoryAdapter adapter;

    @Test
    void savesAndLoadsAnOrderWithItsItems() {
        Order order = Order.create(UUID.randomUUID(), List.of(new OrderItem("a", 2, new BigDecimal("3.50"))), Instant.parse("2026-01-01T10:00:00Z"));
        order.pay();

        adapter.save(order);
        Order loaded = adapter.findById(order.id()).orElseThrow();

        assertThat(loaded.status()).isEqualTo(OrderStatus.PAID);
        assertThat(loaded.customerId()).isEqualTo(order.customerId());
        assertThat(loaded.items()).containsExactly(new OrderItem("a", 2, new BigDecimal("3.50")));
        assertThat(loaded.total()).isEqualByComparingTo("7.00");
    }

    @Test
    void anUnknownIdIsEmpty() {
        assertThat(adapter.findById(UUID.randomUUID())).isEmpty();
    }
}
