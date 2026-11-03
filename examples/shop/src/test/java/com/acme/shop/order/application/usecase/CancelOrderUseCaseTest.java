package com.acme.shop.order.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.shop.order.domain.InvalidOrderException;
import com.acme.shop.order.domain.Order;
import com.acme.shop.order.domain.OrderItem;
import com.acme.shop.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CancelOrderUseCaseTest {

    private final InMemoryOrders orders = new InMemoryOrders();
    private final CancelOrderUseCase useCase = new CancelOrderUseCase(orders);

    private Order stored() {
        return orders.save(Order.create(UUID.randomUUID(), List.of(new OrderItem("sku-1", 1, new BigDecimal("9.99"))), Instant.parse("2026-01-01T10:00:00Z")));
    }

    @Test
    void cancelsANewOrder() {
        Order created = stored();

        useCase.execute(created.id());

        assertThat(orders.findById(created.id()).orElseThrow().status()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void aBrokenRuleSurfacesAndNothingIsSaved() {
        Order created = stored();
        new PayOrderUseCase(orders).execute(created.id());

        assertThatThrownBy(() -> useCase.execute(created.id())).isInstanceOf(InvalidOrderException.class);
        assertThat(orders.findById(created.id()).orElseThrow().status()).isEqualTo(OrderStatus.PAID);
    }
}
