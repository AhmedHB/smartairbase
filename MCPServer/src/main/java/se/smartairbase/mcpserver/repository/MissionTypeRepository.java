package se.smartairbase.mcpserver.repository;

import se.smartairbase.mcpserver.domain.rule.MissionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Loads mission type rule definitions.
 */
public interface MissionTypeRepository extends JpaRepository<MissionType, Long> {

    Optional<MissionType> findByCode(String code);

}
