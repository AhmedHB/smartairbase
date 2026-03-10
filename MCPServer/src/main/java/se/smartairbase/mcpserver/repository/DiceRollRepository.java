package se.smartairbase.mcpserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.smartairbase.mcpserver.domain.game.DiceRoll;

import java.util.List;

public interface DiceRollRepository extends JpaRepository<DiceRoll, Long> {

    List<DiceRoll> findByGameRound_Id(Long gameRoundId);
}
