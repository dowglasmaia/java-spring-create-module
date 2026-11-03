package {{BASE}}.{{feature}}.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import {{BASE}}.{{feature}}.domain.{{Entity}};
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Use case test: a fake of the port and a fixed Clock - no Spring, no database. */
class Create{{Entity}}UseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private final InMemory{{Entity}}s repository = new InMemory{{Entity}}s();
    private final Create{{Entity}}UseCase useCase = new Create{{Entity}}UseCase(repository, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void savesTheNew{{Entity}}StampedWithTheClock() {
        {{Entity}} created = useCase.execute();

        assertThat(created.createdAt()).isEqualTo(NOW);
        assertThat(repository.findById(created.id())).isPresent();
    }
}
