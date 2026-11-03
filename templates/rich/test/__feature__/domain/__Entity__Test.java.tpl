package {{BASE}}.{{feature}}.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Domain test: plain JUnit, no Spring, no mocks. TODO(domain): one test per invariant and transition, valid and invalid. */
class {{Entity}}Test {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void aNew{{Entity}}HasAnIdentityAndTheCreationInstant() {
        {{Entity}} created = {{Entity}}.create(NOW);

        assertThat(created.id()).isNotNull();
        assertThat(created.createdAt()).isEqualTo(NOW);
    }

    @Test
    void restoreRebuildsStoredState() {
        UUID id = UUID.randomUUID();

        assertThat({{Entity}}.restore(id, NOW).id()).isEqualTo(id);
    }
}
