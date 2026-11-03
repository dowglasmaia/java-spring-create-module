package {{BASE}}.{{feature}}.api;

import {{BASE}}.{{feature}}.domain.{{Entity}};
import java.time.Instant;
import java.util.UUID;

/** Response record: the API never exposes the domain or the JPA class. */
public record {{Entity}}Response(UUID id, Instant createdAt) {

    static {{Entity}}Response from({{Entity}} {{entity}}) {
        return new {{Entity}}Response({{entity}}.id(), {{entity}}.createdAt());
    }
}
