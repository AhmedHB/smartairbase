package se.smartairbase.mcpserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.smartairbase.mcpserver.domain.game.GameAnalyticsSnapshot;

import java.util.Optional;

public interface GameAnalyticsSnapshotRepository extends JpaRepository<GameAnalyticsSnapshot, Long> {
    boolean existsByGame_Id(Long gameId);
    Optional<GameAnalyticsSnapshot> findByGame_Id(Long gameId);
}
