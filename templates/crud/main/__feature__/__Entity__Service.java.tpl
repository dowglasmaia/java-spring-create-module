package {{BASE}}.{{feature}};

import {{BASE}}.shared.error.NotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class {{Entity}}Service {

    private final {{Entity}}Repository repository;

    {{Entity}}Service({{Entity}}Repository repository) {
        this.repository = repository;
    }

    @Transactional
    {{Entity}} create(String name) {
        return repository.save(new {{Entity}}(name));
    }

    @Transactional
    {{Entity}} rename(UUID id, String name) {
        {{Entity}} {{entity}} = get(id);
        {{entity}}.rename(name);
        return {{entity}};
    }

    @Transactional(readOnly = true)
    List<{{Entity}}> list() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    {{Entity}} get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("{{Entity}} not found."));
    }

    @Transactional
    void delete(UUID id) {
        repository.delete(get(id));
    }
}
