package se.smartairbase.mcpserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.smartairbase.mcpserver.domain.game.AircraftState;

import java.util.Optional;

/**
 * Loads aircraft state rows for concrete game aircraft.
 */
public interface AircraftStateRepository extends JpaRepository<AircraftState, Long> {

    Optional<AircraftState> findByGameAircraft_Id(Long gameAircraftId);
}
