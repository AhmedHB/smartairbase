package se.smartairbase.mcpserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.smartairbase.mcpserver.domain.game.GameMission;

import java.util.List;
import java.util.Optional;

public interface GameMissionRepository extends JpaRepository<GameMission, Long> {

    List<GameMission> findByGame_IdOrderBySortOrder(Long gameId);

    Optional<GameMission> findByGame_IdAndCode(Long gameId, String code);
}
