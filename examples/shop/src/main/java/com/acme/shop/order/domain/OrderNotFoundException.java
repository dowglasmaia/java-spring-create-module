package com.acme.shop.order.domain;

import com.acme.shop.shared.error.NotFoundException;

public class OrderNotFoundException extends NotFoundException {

    public OrderNotFoundException() {
        super("Order not found.");
    }
}
