package com.acme.shop.order.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.acme.shop.order.domain.Order;
import com.acme.shop.order.domain.OrderItem;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Use case test: an in-memory fake of the port and a fixed Clock - no Spring, no database. */
class CreateOrderUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final UUID CUSTOMER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final InMemoryOrders orders = new InMemoryOrders();
    private final CreateOrderUseCase useCase = new CreateOrderUseCase(orders, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void savesANewOrderStampedWithTheClock() {
        Order created = useCase.execute(CUSTOMER, List.of(new OrderItem("sku-1", 1, new BigDecimal("9.99"))));

        assertThat(created.createdAt()).isEqualTo(NOW);
        assertThat(orders.findById(created.id())).isPresent();
    }
}
