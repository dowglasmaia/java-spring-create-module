package {{BASE}}.{{feature}}.infra.web;

import {{BASE}}.{{feature}}.application.{{Entity}}Service;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thin adapter: translate HTTP to a service call and back. No business logic, no logging, no transactions.
 * TODO(contract): if an OpenAPI-generated interface exists, implement it instead and map its DTOs here (decision D5).
 */
@RestController
@RequestMapping("/{{feature}}")
class {{Entity}}Controller {

    private final {{Entity}}Service service;

    {{Entity}}Controller({{Entity}}Service service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    {{Entity}}Response create() {
        return {{Entity}}Response.from(service.create());
    }

    @GetMapping("/{id}")
    {{Entity}}Response get(@PathVariable UUID id) {
        return {{Entity}}Response.from(service.get(id));
    }
}
