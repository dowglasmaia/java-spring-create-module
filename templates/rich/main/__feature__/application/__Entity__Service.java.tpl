package {{BASE}}.{{feature}}.application;

import {{BASE}}.{{feature}}.domain.{{Entity}};
import {{BASE}}.{{feature}}.domain.{{Entity}}NotFoundException;
import {{BASE}}.{{feature}}.domain.{{Entity}}Repository;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The use cases of the {{feature}} feature in one concrete class (no interface, no class per use case).
 * It orchestrates: load, ask the domain to apply the rule, save, log the business event.
 */
@Service
public class {{Entity}}Service {

    private static final Logger log = LoggerFactory.getLogger({{Entity}}Service.class);

    private final {{Entity}}Repository repository;
    private final Clock clock;

    public {{Entity}}Service({{Entity}}Repository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public {{Entity}} create() {
        {{Entity}} created = repository.save({{Entity}}.create(clock.instant()));
        log.atInfo().addKeyValue("{{entity}}Id", created.id()).log("{{entity}} created");
        return created;
    }

    @Transactional(readOnly = true)
    public {{Entity}} get(UUID id) {
        return repository.findById(id).orElseThrow({{Entity}}NotFoundException::new);
    }
}
