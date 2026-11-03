package com.acme.shop.category;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.acme.shop.shared.error.NotFoundException;
import com.acme.shop.shared.web.GlobalExceptionHandler;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CategoryController.class)
@Import(GlobalExceptionHandler.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CategoryService service;

    @Test
    void createReturns201() throws Exception {
        when(service.create("Books")).thenReturn(new Category("Books"));

        mvc.perform(post("/categories").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Books\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Books"));
    }

    @Test
    void aBlankNameIs422() throws Exception {
        mvc.perform(post("/categories").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void anUnknownCategoryIs404() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.get(id)).thenThrow(new NotFoundException("Category not found."));

        mvc.perform(get("/categories/{id}", id)).andExpect(status().isNotFound());
    }
}
