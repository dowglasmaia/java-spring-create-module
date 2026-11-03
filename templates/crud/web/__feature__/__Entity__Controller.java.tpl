package {{BASE}}.{{feature}};

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/{{feature}}")
class {{Entity}}Controller {

    private final {{Entity}}Service service;

    {{Entity}}Controller({{Entity}}Service service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    {{Entity}}Response create(@Valid @RequestBody {{Entity}}Request request) {
        return {{Entity}}Response.from(service.create(request.name()));
    }

    @GetMapping
    List<{{Entity}}Response> list() {
        return service.list().stream().map({{Entity}}Response::from).toList();
    }

    @GetMapping("/{id}")
    {{Entity}}Response get(@PathVariable UUID id) {
        return {{Entity}}Response.from(service.get(id));
    }

    @PutMapping("/{id}")
    {{Entity}}Response rename(@PathVariable UUID id, @Valid @RequestBody {{Entity}}Request request) {
        return {{Entity}}Response.from(service.rename(id, request.name()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
