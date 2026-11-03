package com.acme.shop.order.application.usecase;

import com.acme.shop.order.application.port.OrderRepository;
import com.acme.shop.order.domain.Order;
import com.acme.shop.order.domain.OrderNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetOrderUseCase {

    private final OrderRepository orders;

    public GetOrderUseCase(OrderRepository orders) {
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public Order execute(UUID id) {
        return orders.findById(id).orElseThrow(OrderNotFoundException::new);
    }
}
