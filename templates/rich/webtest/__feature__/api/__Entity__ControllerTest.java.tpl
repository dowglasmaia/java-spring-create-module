package {{BASE}}.{{feature}}.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import {{BASE}}.{{feature}}.application.usecase.Create{{Entity}}UseCase;
import {{BASE}}.{{feature}}.application.usecase.Get{{Entity}}UseCase;
import {{BASE}}.{{feature}}.domain.{{Entity}};
import {{BASE}}.{{feature}}.domain.{{Entity}}NotFoundException;
import {{BASE}}.shared.web.GlobalExceptionHandler;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** Web slice test: HTTP in, use cases mocked, exception handler imported so error mapping is real. */
@WebMvcTest({{Entity}}Controller.class)
@Import(GlobalExceptionHandler.class)
class {{Entity}}ControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private Create{{Entity}}UseCase create;

    @MockBean
    private Get{{Entity}}UseCase get;

    @Test
    void createReturns201() throws Exception {
        when(create.execute()).thenReturn({{Entity}}.create(Instant.parse("2026-01-01T10:00:00Z")));

        mvc.perform(post("/{{feature}}")).andExpect(status().isCreated()).andExpect(jsonPath("$.id").exists());
    }

    @Test
    void anUnknown{{Entity}}Is404() throws Exception {
        UUID id = UUID.randomUUID();
        when(get.execute(id)).thenThrow(new {{Entity}}NotFoundException());

        mvc.perform(get("/{{feature}}/{id}", id)).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
