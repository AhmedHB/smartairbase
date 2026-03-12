package se.smartairbase.mcpserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.smartairbase.mcpserver.domain.game.Game;

public interface GameRepository extends JpaRepository<Game, Long> {
    boolean existsByNameIgnoreCase(String name);
}
