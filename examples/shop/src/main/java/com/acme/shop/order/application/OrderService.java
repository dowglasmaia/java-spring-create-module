package com.acme.shop.order.application;

import com.acme.shop.order.domain.Order;
import com.acme.shop.order.domain.OrderItem;
import com.acme.shop.order.domain.OrderNotFoundException;
import com.acme.shop.order.domain.OrderRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The use cases of the order feature, in one concrete class (no interface, no class per use case).
 * It orchestrates: load, ask the domain to apply the rule, save, log the business event.
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orders;
    private final Clock clock;

    public OrderService(OrderRepository orders, Clock clock) {
        this.orders = orders;
        this.clock = clock;
    }

    @Transactional
    public Order create(UUID customerId, List<OrderItem> items) {
        Order created = orders.save(Order.create(customerId, items, clock.instant()));
        log.atInfo().addKeyValue("orderId", created.id()).addKeyValue("customerId", customerId).log("order created");
        return created;
    }

    @Transactional
    public Order pay(UUID id) {
        Order order = find(id);
        order.pay();
        Order paid = orders.save(order);
        log.atInfo().addKeyValue("orderId", id).log("order paid");
        return paid;
    }

    @Transactional
    public Order cancel(UUID id) {
        Order order = find(id);
        order.cancel();
        Order cancelled = orders.save(order);
        log.atInfo().addKeyValue("orderId", id).log("order cancelled");
        return cancelled;
    }

    @Transactional(readOnly = true)
    public Order get(UUID id) {
        return find(id);
    }

    private Order find(UUID id) {
        return orders.findById(id).orElseThrow(OrderNotFoundException::new);
    }
}
