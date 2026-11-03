package com.acme.shop.order.application.usecase;

import com.acme.shop.order.application.port.OrderRepository;
import com.acme.shop.order.domain.Order;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** In-memory fake of the output port, shared by the use case tests: no Spring, no database. */
final class InMemoryOrders implements OrderRepository {

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
