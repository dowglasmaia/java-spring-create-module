package {{BASE}}.{{feature}}.api;

import {{BASE}}.{{feature}}.application.usecase.Create{{Entity}}UseCase;
import {{BASE}}.{{feature}}.application.usecase.Get{{Entity}}UseCase;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound adapter: translate HTTP to a use case call and back. No business logic, no logging, no transactions.
 * TODO(contract): if an OpenAPI-generated interface exists, implement it instead and map its DTOs here (decision D5).
 */
@RestController
@RequestMapping("/{{feature}}")
class {{Entity}}Controller {

    private final Create{{Entity}}UseCase create;
    private final Get{{Entity}}UseCase get;

    {{Entity}}Controller(Create{{Entity}}UseCase create, Get{{Entity}}UseCase get) {
        this.create = create;
        this.get = get;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    {{Entity}}Response create() {
        return {{Entity}}Response.from(create.execute());
    }

    @GetMapping("/{id}")
    {{Entity}}Response get(@PathVariable UUID id) {
        return {{Entity}}Response.from(get.execute(id));
    }
}
