package {{BASE}}.{{feature}}.application.usecase;

import {{BASE}}.{{feature}}.application.port.{{Entity}}Repository;
import {{BASE}}.{{feature}}.domain.{{Entity}};
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * One use case = one class = one operation (method {@code execute}). Not final: Spring proxies it for @Transactional.
 * It orchestrates: ask the domain to apply the rule, save through the port, log the business event.
 */
@Service
public class Create{{Entity}}UseCase {

    private static final Logger log = LoggerFactory.getLogger(Create{{Entity}}UseCase.class);

    private final {{Entity}}Repository repository;
    private final Clock clock;

    public Create{{Entity}}UseCase({{Entity}}Repository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public {{Entity}} execute() {
        {{Entity}} created = repository.save({{Entity}}.create(clock.instant()));
        log.atInfo().addKeyValue("{{entity}}Id", created.id()).log("{{entity}} created");
        return created;
    }
}
