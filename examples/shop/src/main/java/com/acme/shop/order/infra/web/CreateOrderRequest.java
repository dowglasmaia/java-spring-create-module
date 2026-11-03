package com.acme.shop.order.infra.web;

import com.acme.shop.order.domain.OrderItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Request record (web contract). Shape checks only; the business rules are in the domain. */
public record CreateOrderRequest(@NotNull UUID customerId, @NotEmpty List<@Valid Item> items) {

    public record Item(@NotBlank String sku, @Positive int quantity, @NotNull @PositiveOrZero BigDecimal unitPrice) {
    }

    List<OrderItem> toDomainItems() {
        return items.stream().map(item -> new OrderItem(item.sku(), item.quantity(), item.unitPrice())).toList();
    }
}
