package se.smartairbase.mcpserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.smartairbase.mcpserver.domain.game.GameRound;

import java.util.List;

public interface GameRoundRepository extends JpaRepository<GameRound, Long> {

    List<GameRound> findByGame_IdOrderByRoundNumber(Long gameId);
}
