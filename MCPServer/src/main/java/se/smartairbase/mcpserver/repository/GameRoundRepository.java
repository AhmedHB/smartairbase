package se.smartairbase.mcpserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.smartairbase.mcpserver.domain.game.GameRound;

import java.util.List;
import java.util.Optional;

/**
 * Stores round rows for game progression.
 */
public interface GameRoundRepository extends JpaRepository<GameRound, Long> {

    List<GameRound> findByGame_IdOrderByRoundNumber(Long gameId);

    Optional<GameRound> findFirstByGame_IdAndEndedAtIsNullOrderByRoundNumberDesc(Long gameId);
}
