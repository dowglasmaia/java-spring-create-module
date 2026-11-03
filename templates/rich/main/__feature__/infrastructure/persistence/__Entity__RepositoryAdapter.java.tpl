package {{BASE}}.{{feature}}.infrastructure.persistence;

import {{BASE}}.{{feature}}.domain.{{Entity}};
import {{BASE}}.{{feature}}.application.port.{{Entity}}Repository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Implements the application port with Spring Data and is the ONLY place that converts Entity <-> Domain. */
@Repository
class {{Entity}}RepositoryAdapter implements {{Entity}}Repository {

    private final {{Entity}}JpaRepository jpa;

    {{Entity}}RepositoryAdapter({{Entity}}JpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public {{Entity}} save({{Entity}} {{entity}}) {
        return toDomain(jpa.save(toEntity({{entity}})));
    }

    @Override
    public Optional<{{Entity}}> findById(UUID id) {
        return jpa.findById(id).map({{Entity}}RepositoryAdapter::toDomain);
    }

    private static {{Entity}}Entity toEntity({{Entity}} {{entity}}) {
        return new {{Entity}}Entity({{entity}}.id(), {{entity}}.createdAt());
    }

    private static {{Entity}} toDomain({{Entity}}Entity entity) {
        return {{Entity}}.restore(entity.getId(), entity.getCreatedAt());
    }
}
