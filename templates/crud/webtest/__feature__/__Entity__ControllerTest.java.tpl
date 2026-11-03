package {{BASE}}.{{feature}};

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import {{BASE}}.shared.error.NotFoundException;
import {{BASE}}.shared.web.GlobalExceptionHandler;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({{Entity}}Controller.class)
@Import(GlobalExceptionHandler.class)
class {{Entity}}ControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private {{Entity}}Service service;

    @Test
    void createReturns201() throws Exception {
        when(service.create("name")).thenReturn(new {{Entity}}("name"));

        mvc.perform(post("/{{feature}}").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"name\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("name"));
    }

    @Test
    void aBlankNameIs422() throws Exception {
        mvc.perform(post("/{{feature}}").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void anUnknown{{Entity}}Is404() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.get(id)).thenThrow(new NotFoundException("{{Entity}} not found."));

        mvc.perform(get("/{{feature}}/{id}", id)).andExpect(status().isNotFound());
    }
}
