package se.smartairbase.mcpserver.repository;

import se.smartairbase.mcpserver.domain.rule.ScenarioSupplyRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Loads supply delivery rules that belong to scenario bases.
 */
public interface ScenarioSupplyRuleRepository extends JpaRepository<ScenarioSupplyRule, Long> {

    List<ScenarioSupplyRule> findByScenarioBase_Id(Long scenarioBaseId);

}
