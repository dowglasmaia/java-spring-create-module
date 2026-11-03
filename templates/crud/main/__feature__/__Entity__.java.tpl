package {{BASE}}.{{feature}};

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * CRUD module (decision D1): no state transition, no invariant, no calculation, no outbound call.
 * The JPA entity is used directly - there is no domain class to protect. If a rule appears, migrate to a rich module.
 */
@Entity
@Table(name = "{{table}}")
public class {{Entity}} {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    protected {{Entity}}() {
        // JPA
    }

    public {{Entity}}(String name) {
        this.id = UUID.randomUUID();
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void rename(String name) {
        this.name = name;
    }
}
