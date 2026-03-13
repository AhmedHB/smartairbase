package se.smartairbase.mcpserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.smartairbase.mcpserver.domain.game.GameEvent;

import java.util.List;

/**
 * Stores the event log emitted while a game progresses.
 */
public interface GameEventRepository extends JpaRepository<GameEvent, Long> {

    List<GameEvent> findByGame_IdOrderByCreatedAtAsc(Long gameId);
}
