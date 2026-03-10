package se.smartairbase.mcpserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.smartairbase.mcpserver.domain.game.AircraftState;

import java.util.Optional;

public interface AircraftStateRepository extends JpaRepository<AircraftState, Long> {

    Optional<AircraftState> findByGameAircraft_Id(Long gameAircraftId);
}
