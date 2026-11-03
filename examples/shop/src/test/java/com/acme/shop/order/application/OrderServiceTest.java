package com.acme.shop.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.acme.shop.order.domain.InvalidOrderException;
import com.acme.shop.order.domain.Order;
import com.acme.shop.order.domain.OrderItem;
import com.acme.shop.order.domain.OrderNotFoundException;
import com.acme.shop.order.domain.OrderRepository;
import com.acme.shop.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Service test: an in-memory fake of the port and a fixed Clock - no Spring, no database. */
class OrderServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final UUID CUSTOMER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final InMemoryOrders orders = new InMemoryOrders();
    private final OrderService service = new OrderService(orders, Clock.fixed(NOW, ZoneOffset.UTC));

    private static List<OrderItem> items() {
        return List.of(new OrderItem("sku-1", 1, new BigDecimal("9.99")));
    }

    @Test
    void createSavesANewOrderStampedWithTheClock() {
        Order created = service.create(CUSTOMER, items());

        assertThat(created.createdAt()).isEqualTo(NOW);
        assertThat(orders.findById(created.id())).isPresent();
    }

    @Test
    void payMarksTheStoredOrderAsPaid() {
        Order created = service.create(CUSTOMER, items());

        service.pay(created.id());

        assertThat(orders.findById(created.id()).orElseThrow().status()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void aBrokenRuleSurfacesAndNothingIsSaved() {
        Order created = service.create(CUSTOMER, items());
        service.pay(created.id());

        assertThatThrownBy(() -> service.cancel(created.id())).isInstanceOf(InvalidOrderException.class);
        assertThat(orders.findById(created.id()).orElseThrow().status()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void anUnknownOrderIsNotFound() {
        assertThatThrownBy(() -> service.get(UUID.randomUUID())).isInstanceOf(OrderNotFoundException.class);
    }

    private static final class InMemoryOrders implements OrderRepository {

        private final Map<UUID, Order> store = new HashMap<>();

        @Override
        public Order save(Order order) {
            store.put(order.id(), order);
            return order;
        }

        @Override
        public Optional<Order> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }
    }
}
