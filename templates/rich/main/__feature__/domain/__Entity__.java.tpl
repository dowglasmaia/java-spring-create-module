package {{BASE}}.{{feature}}.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * {{Entity}}: the business rules live here. Pure Java - no Spring, no JPA, no Lombok, no logging.
 * Built through {@link #create} (applies the creation rules) or {@link #restore} (rebuilds a stored one); no setters.
 *
 * TODO(domain): add the business fields, the invariants (in create) and the intention methods (pay, cancel, ...).
 */
public final class {{Entity}} {

    private final UUID id;
    private final Instant createdAt;

    private {{Entity}}(UUID id, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static {{Entity}} create(Instant now) {
        return new {{Entity}}(UUID.randomUUID(), now);
    }

    public static {{Entity}} restore(UUID id, Instant createdAt) {
        return new {{Entity}}(id, createdAt);
    }

    public UUID id() {
        return id;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
