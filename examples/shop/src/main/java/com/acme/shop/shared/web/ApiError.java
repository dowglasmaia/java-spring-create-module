package com.acme.shop.shared.web;

/** Error body of every failed call: {@code {code, message}}. */
public record ApiError(String code, String message) {
}
