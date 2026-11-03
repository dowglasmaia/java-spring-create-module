package {{BASE}}.{{feature}}.infra.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface {{Entity}}JpaRepository extends JpaRepository<{{Entity}}Entity, UUID> {
}
