package se.smartairbase.mcpserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.smartairbase.mcpserver.domain.game.GameBase;

import java.util.List;
import java.util.Optional;

/**
 * Persists base rows that belong to concrete games.
 */
public interface GameBaseRepository extends JpaRepository<GameBase, Long> {

    List<GameBase> findByGame_Id(Long gameId);

    Optional<GameBase> findByGame_IdAndCode(Long gameId, String code);
}
