package se.smartairbase.mcpserver.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.smartairbase.mcpserver.domain.game.AircraftState;
import se.smartairbase.mcpserver.domain.game.GameAircraft;
import se.smartairbase.mcpserver.domain.game.enums.AircraftStatus;
import se.smartairbase.mcpserver.mcp.dto.AircraftStateDto;
import se.smartairbase.mcpserver.repository.AircraftStateRepository;
import se.smartairbase.mcpserver.repository.GameAircraftRepository;

import java.util.ArrayList;
import java.util.List;

@Service
/**
 * Read service for one aircraft's current runtime state.
 */
public class AircraftService {

    private final GameAircraftRepository gameAircraftRepository;
    private final AircraftStateRepository aircraftStateRepository;

    public AircraftService(GameAircraftRepository gameAircraftRepository,
                           AircraftStateRepository aircraftStateRepository) {
        this.gameAircraftRepository = gameAircraftRepository;
        this.aircraftStateRepository = aircraftStateRepository;
    }

    /**
     * Returns the persisted state for a single aircraft together with the
     * actions the client may legally offer next.
     */
    @Transactional(readOnly = true)
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
                state.getAssignedMission() != null ? state.getAssignedMission().getCode() : null,
                state.getLastDiceValue(),
                allowedActions(aircraft)
        );
    }

    private List<String> allowedActions(GameAircraft aircraft) {
        List<String> actions = new ArrayList<>();
        if (aircraft.getStatus() == AircraftStatus.READY) {
            actions.add("ASSIGN_MISSION");
        }
        if (aircraft.getStatus() == AircraftStatus.AWAITING_DICE_ROLL) {
            actions.add("RECORD_DICE_ROLL");
        }
        if (aircraft.getStatus() == AircraftStatus.AWAITING_LANDING) {
            actions.add("LAND");
            actions.add("SEND_TO_HOLDING");
        }
        return actions;
    }
}
