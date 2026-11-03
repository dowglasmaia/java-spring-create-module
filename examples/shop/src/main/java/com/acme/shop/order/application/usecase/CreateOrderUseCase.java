package com.acme.shop.order.application.usecase;

import com.acme.shop.order.application.port.OrderRepository;
import com.acme.shop.order.domain.Order;
import com.acme.shop.order.domain.OrderItem;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * One use case = one class = one operation (method {@code execute}). Not final: Spring proxies it for @Transactional.
 * It orchestrates: ask the domain to apply the rule, save through the port, log the business event.
 */
@Service
public class CreateOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreateOrderUseCase.class);

    private final OrderRepository orders;
    private final Clock clock;

    public CreateOrderUseCase(OrderRepository orders, Clock clock) {
        this.orders = orders;
        this.clock = clock;
    }

    @Transactional
    public Order execute(UUID customerId, List<OrderItem> items) {
        Order created = orders.save(Order.create(customerId, items, clock.instant()));
        log.atInfo().addKeyValue("orderId", created.id()).addKeyValue("customerId", customerId).log("order created");
        return created;
    }
}
