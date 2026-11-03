package com.acme.shop.category;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * CRUD module (decision D1): no state transition, no invariant, no calculation, no outbound call.
 * The JPA entity is used directly - there is no domain class to protect. The module is one flat package.
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    protected Category() {
        // JPA
    }

    public Category(String name) {
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
