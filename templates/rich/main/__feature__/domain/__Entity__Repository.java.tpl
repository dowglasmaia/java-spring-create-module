package {{BASE}}.{{feature}}.domain;

import java.util.Optional;
import java.util.UUID;

/** OUTPUT PORT: what the {{feature}} feature needs from storage. Implemented by the persistence adapter. */
public interface {{Entity}}Repository {

    {{Entity}} save({{Entity}} {{entity}});

    Optional<{{Entity}}> findById(UUID id);
}
