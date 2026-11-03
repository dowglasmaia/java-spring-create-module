package {{BASE}}.{{feature}}.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import {{BASE}}.{{feature}}.domain.{{Entity}};
import {{BASE}}.{{feature}}.domain.{{Entity}}NotFoundException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class Get{{Entity}}UseCaseTest {

    private final InMemory{{Entity}}s repository = new InMemory{{Entity}}s();
    private final Get{{Entity}}UseCase useCase = new Get{{Entity}}UseCase(repository);

    @Test
    void returnsTheStored{{Entity}}() {
        {{Entity}} stored = repository.save({{Entity}}.create(Instant.parse("2026-01-01T10:00:00Z")));

        assertThat(useCase.execute(stored.id())).isEqualTo(stored);
    }

    @Test
    void anUnknown{{Entity}}IsNotFound() {
        assertThatThrownBy(() -> useCase.execute(UUID.randomUUID())).isInstanceOf({{Entity}}NotFoundException.class);
    }
}
