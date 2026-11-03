package com.acme.shop.order;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** End to end through every layer: create -> pay -> cancel is rejected, with the real database. */
@SpringBootTest
@AutoConfigureMockMvc
class OrderFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void createPayThenCancelIsRejected() throws Exception {
        String body = "{\"customerId\":\"" + UUID.randomUUID() + "\",\"items\":[{\"sku\":\"sku-1\",\"quantity\":1,\"unitPrice\":5.00}]}";
        String created = mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = json.readTree(created).get("id").asText();

        mvc.perform(post("/orders/{id}/pay", id)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
        mvc.perform(post("/orders/{id}/cancel", id)).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Only a new order can be cancelled."));
    }
}
