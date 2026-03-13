package se.smartairbase.mcpserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.smartairbase.mcpserver.domain.game.GameAnalysisEntry;

import java.util.List;

/**
 * Stores persisted analysis feed entries for games.
 */
public interface GameAnalysisEntryRepository extends JpaRepository<GameAnalysisEntry, Long> {

    List<GameAnalysisEntry> findByGame_IdOrderByCreatedAtAscIdAsc(Long gameId);

    boolean existsByGame_IdAndRoundNumberAndRoleName(Long gameId, Integer roundNumber, String roleName);
}
