package com.acme.shop.order.application.usecase;

import com.acme.shop.order.application.port.OrderRepository;
import com.acme.shop.order.domain.Order;
import com.acme.shop.order.domain.OrderNotFoundException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(PayOrderUseCase.class);

    private final OrderRepository orders;

    public PayOrderUseCase(OrderRepository orders) {
        this.orders = orders;
    }

    @Transactional
    public Order execute(UUID id) {
        Order order = orders.findById(id).orElseThrow(OrderNotFoundException::new);
        order.pay();
        Order paid = orders.save(order);
        log.atInfo().addKeyValue("orderId", id).log("order paid");
        return paid;
    }
}
