package com.acme.shop.category;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data used directly by the service: a CRUD module needs no port. */
interface CategoryRepository extends JpaRepository<Category, UUID> {
}
