package {{BASE}}.{{feature}};

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import {{BASE}}.shared.error.NotFoundException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class {{Entity}}ServiceTest {

    @Mock
    private {{Entity}}Repository repository;

    @InjectMocks
    private {{Entity}}Service service;

    @Test
    void createSavesTheNew{{Entity}}() {
        when(repository.save(any({{Entity}}.class))).thenAnswer(call -> call.getArgument(0));

        assertThat(service.create("name").getName()).isEqualTo("name");
    }

    @Test
    void anUnknown{{Entity}}IsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id)).isInstanceOf(NotFoundException.class);
    }
}
