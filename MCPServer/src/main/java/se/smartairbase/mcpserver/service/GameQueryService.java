package se.smartairbase.mcpserver.service;

import org.springframework.stereotype.Service;
import se.smartairbase.mcpserver.domain.game.*;
import se.smartairbase.mcpserver.domain.game.enums.AircraftStatus;
import se.smartairbase.mcpserver.domain.game.enums.GameStatus;
import se.smartairbase.mcpserver.mcp.dto.*;
import se.smartairbase.mcpserver.repository.*;

import java.util.ArrayList;
import java.util.List;

@Service
/**
 * Read-focused facade for assembling a client-friendly view of the current game.
 *
 * <p>This service does not change state. It derives useful control information
 * such as current round phase and allowed actions so the MCP client can drive
 * the game without duplicating rule logic.</p>
 */
public class GameQueryService {

    private final GameRepository gameRepository;
    private final GameBaseRepository gameBaseRepository;
    private final GameAircraftRepository gameAircraftRepository;
    private final GameMissionRepository gameMissionRepository;
    private final BaseStateRepository baseStateRepository;
    private final AircraftStateRepository aircraftStateRepository;
    private final GameEventRepository gameEventRepository;
    private final ResourceTransactionRepository resourceTransactionRepository;
    private final GameRoundRepository gameRoundRepository;

    public GameQueryService(GameRepository gameRepository,
                            GameBaseRepository gameBaseRepository,
                            GameAircraftRepository gameAircraftRepository,
                            GameMissionRepository gameMissionRepository,
                            BaseStateRepository baseStateRepository,
                            AircraftStateRepository aircraftStateRepository,
                            GameEventRepository gameEventRepository,
                            ResourceTransactionRepository resourceTransactionRepository,
                            GameRoundRepository gameRoundRepository) {
        this.gameRepository = gameRepository;
        this.gameBaseRepository = gameBaseRepository;
        this.gameAircraftRepository = gameAircraftRepository;
        this.gameMissionRepository = gameMissionRepository;
        this.baseStateRepository = baseStateRepository;
        this.aircraftStateRepository = aircraftStateRepository;
        this.gameEventRepository = gameEventRepository;
        this.resourceTransactionRepository = resourceTransactionRepository;
        this.gameRoundRepository = gameRoundRepository;
    }

    /**
     * Builds the consolidated game state returned to MCP clients.
     *
     * <p>The result includes summary data, base state, aircraft state, mission
     * state and round-level control flags.</p>
     */
    public GameStateDto getGameState(Long gameId) {
        Game game = gameRepository.findById(gameId).orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));
        GameRound activeRound = gameRoundRepository.findFirstByGame_IdAndEndedAtIsNullOrderByRoundNumberDesc(gameId).orElse(null);
        boolean canCompleteRound = activeRound != null && !hasPendingRoundDecisions(gameId);

        List<BaseStateDto> bases = gameBaseRepository.findByGame_Id(gameId).stream()
                .map(base -> {
                    BaseState state = baseStateRepository.findByGameBase_Id(base.getId()).orElseThrow();
                    return new BaseStateDto(base.getCode(), base.getName(), base.getBaseType().getCode(),
                            state.getFuelStock(), state.getWeaponsStock(), state.getSparePartsStock(),
                            state.getOccupiedParkingSlots(), base.getParkingCapacity(),
                            state.getOccupiedMaintSlots(), base.getMaintenanceCapacity());
                })
                .toList();

        List<AircraftStateDto> aircraft = gameAircraftRepository.findByGame_Id(gameId).stream()
                .map(a -> {
                    AircraftState state = aircraftStateRepository.findByGameAircraft_Id(a.getId()).orElseThrow();
                    return new AircraftStateDto(a.getCode(), a.getStatus().name(),
                            state.getCurrentBase() != null ? state.getCurrentBase().getCode() : null,
                            state.getFuel(), state.getWeapons(), state.getRemainingFlightHours(),
                            state.getDamage().name(), state.getRepairRoundsRemaining(), state.isInHolding(),
                            state.getAssignedMission() != null ? state.getAssignedMission().getCode() : null,
                            state.getLastDiceValue(), allowedActions(a));
                })
                .toList();

        List<MissionStateDto> missions = gameMissionRepository.findByGame_IdOrderBySortOrder(gameId).stream()
                .map(m -> new MissionStateDto(m.getCode(), m.getMissionType().getCode(), m.getStatus().name(),
                        m.getSortOrder(), missionBlocker(m)))
                .toList();

        GameSummaryDto summary = new GameSummaryDto(game.getId(), game.getName(), game.getScenario().getName(),
                game.getScenario().getVersion(), game.getStatus().name(), game.getCurrentRound(),
                activeRound != null ? activeRound.getPhase().name() : null,
                activeRound != null,
                game.getStatus() == GameStatus.ACTIVE && activeRound == null,
                canCompleteRound);

        return new GameStateDto(summary, bases, aircraft, missions,
                gameEventRepository.findByGame_IdOrderByCreatedAtAsc(gameId).size(),
                resourceTransactionRepository.findByGame_Id(gameId).size());
    }

    private List<String> allowedActions(GameAircraft aircraft) {
        List<String> actions = new ArrayList<>();
        AircraftStatus status = aircraft.getStatus();
        if (status == AircraftStatus.READY) {
            actions.add("ASSIGN_MISSION");
        }
        if (status == AircraftStatus.AWAITING_DICE_ROLL) {
            actions.add("RECORD_DICE_ROLL");
        }
        if (status == AircraftStatus.AWAITING_LANDING) {
            actions.add("LAND");
            actions.add("SEND_TO_HOLDING");
        }
        return actions;
    }

    private String missionBlocker(GameMission mission) {
        if (mission.getStatus().name().equals("AVAILABLE")) {
            return null;
        }
        return "Mission status is " + mission.getStatus().name();
    }

    private boolean hasPendingRoundDecisions(Long gameId) {
        return gameAircraftRepository.findByGame_Id(gameId).stream()
                .map(GameAircraft::getStatus)
                .anyMatch(status -> status == AircraftStatus.AWAITING_DICE_ROLL || status == AircraftStatus.AWAITING_LANDING);
    }
}
