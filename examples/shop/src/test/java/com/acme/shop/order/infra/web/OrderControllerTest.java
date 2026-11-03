package com.acme.shop.order.infra.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.shop.order.application.OrderService;
import com.acme.shop.order.domain.InvalidOrderException;
import com.acme.shop.order.domain.Order;
import com.acme.shop.order.domain.OrderItem;
import com.acme.shop.order.domain.OrderNotFoundException;
import com.acme.shop.shared.web.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Web slice test: HTTP in, service mocked, exception handler imported so error mapping is real. */
@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    private static final UUID CUSTOMER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mvc;

    @MockBean
    private OrderService service;

    private static Order order() {
        return Order.create(CUSTOMER, List.of(new OrderItem("sku-1", 2, new BigDecimal("10.00"))), Instant.parse("2026-01-01T10:00:00Z"));
    }

    @Test
    void createReturns201AndTheOrder() throws Exception {
        when(service.create(any(), any())).thenReturn(order());

        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"" + CUSTOMER + "\",\"items\":[{\"sku\":\"sku-1\",\"quantity\":2,\"unitPrice\":10.00}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.total").value(20.00));
    }

    @Test
    void aMalformedBodyIs422WithTheStandardErrorShape() throws Exception {
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("UNPROCESSABLE_ENTITY"));
    }

    @Test
    void aBrokenBusinessRuleIs422() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.cancel(id)).thenThrow(new InvalidOrderException("Only a new order can be cancelled."));

        mvc.perform(post("/orders/{id}/cancel", id))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Only a new order can be cancelled."));
    }

    @Test
    void anUnknownOrderIs404() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.get(id)).thenThrow(new OrderNotFoundException());

        mvc.perform(get("/orders/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
