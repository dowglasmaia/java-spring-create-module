package {{BASE}}.{{feature}}.application.port;

import {{BASE}}.{{feature}}.domain.{{Entity}};
import java.util.Optional;
import java.util.UUID;

/** OUTPUT PORT: what the {{feature}} feature needs from storage. Implemented by the persistence adapter in infrastructure. */
public interface {{Entity}}Repository {

    {{Entity}} save({{Entity}} {{entity}});

    Optional<{{Entity}}> findById(UUID id);
}
