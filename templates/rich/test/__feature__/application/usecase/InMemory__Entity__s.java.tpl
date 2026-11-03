package {{BASE}}.{{feature}}.application.usecase;

import {{BASE}}.{{feature}}.application.port.{{Entity}}Repository;
import {{BASE}}.{{feature}}.domain.{{Entity}};
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** In-memory fake of the output port, shared by the use case tests: no Spring, no database. */
final class InMemory{{Entity}}s implements {{Entity}}Repository {

    private final Map<UUID, {{Entity}}> store = new HashMap<>();

    @Override
    public {{Entity}} save({{Entity}} {{entity}}) {
        store.put({{entity}}.id(), {{entity}});
        return {{entity}};
    }

    @Override
    public Optional<{{Entity}}> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }
}
