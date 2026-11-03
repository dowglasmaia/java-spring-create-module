package com.acme.shop.order.domain;

import com.acme.shop.shared.error.BusinessRuleException;

/** An order rule was broken (invariant or illegal state change). */
public class InvalidOrderException extends BusinessRuleException {

    public InvalidOrderException(String message) {
        super(message);
    }
}
