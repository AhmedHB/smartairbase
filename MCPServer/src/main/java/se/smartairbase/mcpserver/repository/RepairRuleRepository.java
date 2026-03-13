package se.smartairbase.mcpserver.repository;

import se.smartairbase.mcpserver.domain.rule.RepairRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Loads dice-to-damage repair rules.
 */
public interface RepairRuleRepository extends JpaRepository<RepairRule, Long> {

    Optional<RepairRule> findByDiceValue(Integer diceValue);

    Optional<RepairRule> findByRequiresFullServiceTrue();

}
