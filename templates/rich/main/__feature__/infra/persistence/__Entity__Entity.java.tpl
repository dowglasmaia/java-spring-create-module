package {{BASE}}.{{feature}}.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** JPA entity: columns and mappings only, no business rule. Lives in infra; the domain never sees it. */
@Entity
@Table(name = "{{table}}")
public class {{Entity}}Entity {

    @Id
    private UUID id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected {{Entity}}Entity() {
        // JPA
    }

    public {{Entity}}Entity(UUID id, Instant createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
