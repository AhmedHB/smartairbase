package se.smartairbase.mcpserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.smartairbase.mcpserver.domain.game.DiceRoll;

import java.util.List;
import java.util.Optional;

/**
 * Stores dice roll outcomes recorded during games.
 */
public interface DiceRollRepository extends JpaRepository<DiceRoll, Long> {

    List<DiceRoll> findByGameRound_Id(Long gameRoundId);

    List<DiceRoll> findByGameRound_Game_IdOrderByRolledAtAsc(Long gameId);

    Optional<DiceRoll> findByGameRound_IdAndGameAircraft_Id(Long gameRoundId, Long gameAircraftId);
}
