package com.acme.shop.shared.error;

/** A business rule was broken. Mapped to HTTP 422 by the global exception handler. Plain Java: the domain may use it. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
