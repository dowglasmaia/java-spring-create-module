package com.acme.shop.order.api;

import com.acme.shop.order.domain.Order;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Response record: the API never exposes the domain or the JPA class. */
public record OrderResponse(UUID id, UUID customerId, String status, BigDecimal total, Instant createdAt) {

    static OrderResponse from(Order order) {
        return new OrderResponse(order.id(), order.customerId(), order.status().name(), order.total(), order.createdAt());
    }
}
