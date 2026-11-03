package com.acme.shop.order.application.port;

import com.acme.shop.order.domain.Order;
import java.util.Optional;
import java.util.UUID;

/** OUTPUT PORT: what the order feature needs from storage. Implemented by the persistence adapter in infrastructure. */
public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(UUID id);
}
