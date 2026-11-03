package {{BASE}}.{{feature}}.application.usecase;

import {{BASE}}.{{feature}}.application.port.{{Entity}}Repository;
import {{BASE}}.{{feature}}.domain.{{Entity}};
import {{BASE}}.{{feature}}.domain.{{Entity}}NotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Get{{Entity}}UseCase {

    private final {{Entity}}Repository repository;

    public Get{{Entity}}UseCase({{Entity}}Repository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public {{Entity}} execute(UUID id) {
        return repository.findById(id).orElseThrow({{Entity}}NotFoundException::new);
    }
}
