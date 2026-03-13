package se.smartairbase.mcpserver.repository;

import se.smartairbase.mcpserver.domain.rule.Scenario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

/**
 * Loads scenario templates and editable custom scenarios.
 */
public interface ScenarioRepository extends JpaRepository<Scenario, Long> {
    Optional<Scenario> findByNameAndVersion(String name, String version);
    boolean existsByNameAndVersion(String name, String version);
    List<Scenario> findAllByOrderByNameAscVersionAsc();
}
