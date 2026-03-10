package se.smartairbase.mcpserver.service;

import org.springframework.stereotype.Service;
import se.smartairbase.mcpserver.domain.game.AircraftState;
import se.smartairbase.mcpserver.domain.game.GameAircraft;
import se.smartairbase.mcpserver.mcp.dto.AircraftStateDto;
import se.smartairbase.mcpserver.repository.AircraftStateRepository;
import se.smartairbase.mcpserver.repository.GameAircraftRepository;

@Service
public class AircraftService {

    private final GameAircraftRepository gameAircraftRepository;
    private final AircraftStateRepository aircraftStateRepository;

    public AircraftService(GameAircraftRepository gameAircraftRepository,
                           AircraftStateRepository aircraftStateRepository) {
        this.gameAircraftRepository = gameAircraftRepository;
        this.aircraftStateRepository = aircraftStateRepository;
    }

    public AircraftStateDto getAircraftState(Long gameId, String aircraftCode) {
        GameAircraft aircraft = gameAircraftRepository.findByGame_IdAndCode(gameId, aircraftCode)
                .orElseThrow(() -> new IllegalArgumentException("Aircraft not found: " + aircraftCode));
        AircraftState state = aircraftStateRepository.findByGameAircraft_Id(aircraft.getId())
                .orElseThrow(() -> new IllegalArgumentException("Aircraft state not found: " + aircraftCode));

        return new AircraftStateDto(
                aircraft.getCode(),
                aircraft.getStatus().name(),
                state.getCurrentBase() != null ? state.getCurrentBase().getCode() : null,
                state.getFuel(),
                state.getWeapons(),
                state.getRemainingFlightHours(),
                state.getDamage().name(),
                state.getRepairRoundsRemaining(),
                state.isInHolding(),
                state.getAssignedMission() != null ? state.getAssignedMission().getCode() : null
        );
    }
}
