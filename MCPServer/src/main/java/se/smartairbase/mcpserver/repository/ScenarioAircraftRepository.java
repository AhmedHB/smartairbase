package se.smartairbase.mcpserver.repository;

import se.smartairbase.mcpserver.domain.rule.ScenarioAircraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScenarioAircraftRepository extends JpaRepository<ScenarioAircraft, Long> {

    List<ScenarioAircraft> findByScenario_Id(Long scenarioId);

    Optional<ScenarioAircraft> findByScenario_IdAndCode(Long scenarioId, String code);

}
