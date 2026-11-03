package {{BASE}}.{{feature}}.infra.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import {{BASE}}.{{feature}}.domain.{{Entity}};
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * Adapter test: Domain -> Entity -> database -> Entity -> Domain.
 * Uses the embedded database of @DataJpaTest; if the project tests against Testcontainers, switch to that base class.
 */
@DataJpaTest
@Import({{Entity}}RepositoryAdapter.class)
class {{Entity}}RepositoryAdapterTest {

    @Autowired
    private {{Entity}}RepositoryAdapter adapter;

    @Test
    void savesAndLoadsA{{Entity}}() {
        {{Entity}} {{entity}} = {{Entity}}.create(Instant.parse("2026-01-01T10:00:00Z"));

        adapter.save({{entity}});

        assertThat(adapter.findById({{entity}}.id())).get().satisfies(loaded -> assertThat(loaded.createdAt()).isEqualTo({{entity}}.createdAt()));
    }

    @Test
    void anUnknownIdIsEmpty() {
        assertThat(adapter.findById(UUID.randomUUID())).isEmpty();
    }
}
