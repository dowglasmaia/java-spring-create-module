package com.acme.shop.order.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.shop.order.domain.Order;
import com.acme.shop.order.domain.OrderItem;
import com.acme.shop.order.domain.OrderNotFoundException;
import com.acme.shop.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PayOrderUseCaseTest {

    private final InMemoryOrders orders = new InMemoryOrders();
    private final PayOrderUseCase useCase = new PayOrderUseCase(orders);

    @Test
    void marksTheStoredOrderAsPaid() {
        Order created = orders.save(Order.create(UUID.randomUUID(), List.of(new OrderItem("sku-1", 1, new BigDecimal("9.99"))), Instant.parse("2026-01-01T10:00:00Z")));

        useCase.execute(created.id());

        assertThat(orders.findById(created.id()).orElseThrow().status()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void anUnknownOrderIsNotFound() {
        assertThatThrownBy(() -> useCase.execute(UUID.randomUUID())).isInstanceOf(OrderNotFoundException.class);
    }
}
