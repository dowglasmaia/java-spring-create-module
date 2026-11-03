package com.acme.shop.shared.error;

/** A requested resource does not exist (or is not visible to the caller). Mapped to HTTP 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
