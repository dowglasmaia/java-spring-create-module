package com.acme.shop.order.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity with the business rules. Pure Java: no Spring, no JPA, no Lombok, no logging.
 * Built through {@link #create} (applies the rules) or {@link #restore} (rebuilds a stored order); there are no setters.
 */
public final class Order {

    private final UUID id;
    private final UUID customerId;
    private final List<OrderItem> items;
    private final Instant createdAt;
    private OrderStatus status;

    private Order(UUID id, UUID customerId, List<OrderItem> items, Instant createdAt, OrderStatus status) {
        this.id = Objects.requireNonNull(id, "id");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.items = List.copyOf(items);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.status = Objects.requireNonNull(status, "status");
    }

    public static Order create(UUID customerId, List<OrderItem> items, Instant now) {
        if (items == null || items.isEmpty()) {
            throw new InvalidOrderException("An order needs at least one item.");
        }
        return new Order(UUID.randomUUID(), customerId, items, now, OrderStatus.NEW);
    }

    public static Order restore(UUID id, UUID customerId, List<OrderItem> items, Instant createdAt, OrderStatus status) {
        return new Order(id, customerId, items, createdAt, status);
    }

    public void pay() {
        if (status != OrderStatus.NEW) {
            throw new InvalidOrderException("Only a new order can be paid.");
        }
        status = OrderStatus.PAID;
    }

    public void cancel() {
        if (status != OrderStatus.NEW) {
            throw new InvalidOrderException("Only a new order can be cancelled.");
        }
        status = OrderStatus.CANCELLED;
    }

    public BigDecimal total() {
        return items.stream().map(OrderItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public UUID id() {
        return id;
    }

    public UUID customerId() {
        return customerId;
    }

    public List<OrderItem> items() {
        return items;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public OrderStatus status() {
        return status;
    }
}
