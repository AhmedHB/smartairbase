package se.smartairbase.mcpserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.smartairbase.mcpserver.domain.game.DiceRoll;

import java.util.List;
import java.util.Optional;

public interface DiceRollRepository extends JpaRepository<DiceRoll, Long> {

    List<DiceRoll> findByGameRound_Id(Long gameRoundId);

    Optional<DiceRoll> findByGameRound_IdAndGameAircraft_Id(Long gameRoundId, Long gameAircraftId);
}
