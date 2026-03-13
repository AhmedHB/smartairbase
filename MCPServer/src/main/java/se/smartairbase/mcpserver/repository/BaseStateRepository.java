package se.smartairbase.mcpserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.smartairbase.mcpserver.domain.game.BaseState;

import java.util.Optional;

/**
 * Loads base state rows for concrete game bases.
 */
public interface BaseStateRepository extends JpaRepository<BaseState, Long> {

    Optional<BaseState> findByGameBase_Id(Long gameBaseId);
}
