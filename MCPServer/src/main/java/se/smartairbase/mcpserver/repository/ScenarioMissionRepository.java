package se.smartairbase.mcpserver.repository;

import se.smartairbase.mcpserver.domain.rule.ScenarioMission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Loads mission entries that belong to scenario templates.
 */
public interface ScenarioMissionRepository extends JpaRepository<ScenarioMission, Long> {

    List<ScenarioMission> findByScenario_IdOrderBySortOrder(Long scenarioId);

}
