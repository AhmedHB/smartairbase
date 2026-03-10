package se.smartairbase.mcpserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.smartairbase.mcpserver.domain.game.GameAircraft;

import java.util.List;
import java.util.Optional;

public interface GameAircraftRepository extends JpaRepository<GameAircraft, Long> {

    List<GameAircraft> findByGame_Id(Long gameId);

    Optional<GameAircraft> findByGame_IdAndCode(Long gameId, String code);
}
