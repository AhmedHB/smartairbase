package se.smartairbase.mcpserver.repository;

import se.smartairbase.mcpserver.domain.rule.ScenarioBase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Loads base entries that belong to scenario templates.
 */
public interface ScenarioBaseRepository extends JpaRepository<ScenarioBase, Long> {

    List<ScenarioBase> findByScenario_Id(Long scenarioId);

    Optional<ScenarioBase> findByScenario_IdAndCode(Long scenarioId, String code);

}
