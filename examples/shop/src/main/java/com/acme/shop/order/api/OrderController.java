package com.acme.shop.order.api;

import com.acme.shop.order.application.usecase.CancelOrderUseCase;
import com.acme.shop.order.application.usecase.CreateOrderUseCase;
import com.acme.shop.order.application.usecase.GetOrderUseCase;
import com.acme.shop.order.application.usecase.PayOrderUseCase;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Inbound adapter: translate HTTP to a use case call and back. No business logic, no logging, no transactions. */
@RestController
@RequestMapping("/orders")
class OrderController {

    private final CreateOrderUseCase createOrder;
    private final GetOrderUseCase getOrder;
    private final PayOrderUseCase payOrder;
    private final CancelOrderUseCase cancelOrder;

    OrderController(CreateOrderUseCase createOrder, GetOrderUseCase getOrder, PayOrderUseCase payOrder, CancelOrderUseCase cancelOrder) {
        this.createOrder = createOrder;
        this.getOrder = getOrder;
        this.payOrder = payOrder;
        this.cancelOrder = cancelOrder;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return OrderResponse.from(createOrder.execute(request.customerId(), request.toDomainItems()));
    }

    @GetMapping("/{id}")
    OrderResponse get(@PathVariable UUID id) {
        return OrderResponse.from(getOrder.execute(id));
    }

    @PostMapping("/{id}/pay")
    OrderResponse pay(@PathVariable UUID id) {
        return OrderResponse.from(payOrder.execute(id));
    }

    @PostMapping("/{id}/cancel")
    OrderResponse cancel(@PathVariable UUID id) {
        return OrderResponse.from(cancelOrder.execute(id));
    }
}
