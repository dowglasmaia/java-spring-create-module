package com.acme.shop.order.infra.persistence;

import com.acme.shop.order.domain.Order;
import com.acme.shop.order.domain.OrderItem;
import com.acme.shop.order.domain.OrderRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Implements the domain port with Spring Data and is the ONLY place that converts Entity <-> Domain. */
@Repository
class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpa;

    OrderRepositoryAdapter(OrderJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Order save(Order order) {
        return toDomain(jpa.save(toEntity(order)));
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return jpa.findById(id).map(OrderRepositoryAdapter::toDomain);
    }

    private static OrderEntity toEntity(Order order) {
        return new OrderEntity(order.id(), order.customerId(), order.status(), order.createdAt(),
                order.items().stream().map(item -> new OrderItemEmbeddable(item.sku(), item.quantity(), item.unitPrice())).toList());
    }

    private static Order toDomain(OrderEntity entity) {
        return Order.restore(entity.getId(), entity.getCustomerId(),
                entity.getItems().stream().map(item -> new OrderItem(item.getSku(), item.getQuantity(), item.getUnitPrice())).toList(),
                entity.getCreatedAt(), entity.getStatus());
    }
}
