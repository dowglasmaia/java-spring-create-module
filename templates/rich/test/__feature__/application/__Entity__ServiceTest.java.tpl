package {{BASE}}.{{feature}}.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import {{BASE}}.{{feature}}.domain.{{Entity}};
import {{BASE}}.{{feature}}.domain.{{Entity}}NotFoundException;
import {{BASE}}.{{feature}}.domain.{{Entity}}Repository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Service test: an in-memory fake of the port and a fixed Clock - no Spring, no database. */
class {{Entity}}ServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private final InMemory{{Entity}}s repository = new InMemory{{Entity}}s();
    private final {{Entity}}Service service = new {{Entity}}Service(repository, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void createSavesTheNew{{Entity}}StampedWithTheClock() {
        {{Entity}} created = service.create();

        assertThat(created.createdAt()).isEqualTo(NOW);
        assertThat(repository.findById(created.id())).isPresent();
    }

    @Test
    void anUnknown{{Entity}}IsNotFound() {
        assertThatThrownBy(() -> service.get(UUID.randomUUID())).isInstanceOf({{Entity}}NotFoundException.class);
    }

    private static final class InMemory{{Entity}}s implements {{Entity}}Repository {

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
}
