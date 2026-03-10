package se.smartairbase.mcpserver.service;

import org.springframework.stereotype.Service;
import se.smartairbase.mcpserver.domain.game.*;
import se.smartairbase.mcpserver.mcp.dto.*;
import se.smartairbase.mcpserver.repository.*;

import java.util.List;

@Service
public class GameQueryService {

    private final GameRepository gameRepository;
    private final GameBaseRepository gameBaseRepository;
    private final GameAircraftRepository gameAircraftRepository;
    private final GameMissionRepository gameMissionRepository;
    private final BaseStateRepository baseStateRepository;
    private final AircraftStateRepository aircraftStateRepository;
    private final GameEventRepository gameEventRepository;
    private final ResourceTransactionRepository resourceTransactionRepository;

    public GameQueryService(GameRepository gameRepository,
                            GameBaseRepository gameBaseRepository,
                            GameAircraftRepository gameAircraftRepository,
                            GameMissionRepository gameMissionRepository,
                            BaseStateRepository baseStateRepository,
                            AircraftStateRepository aircraftStateRepository,
                            GameEventRepository gameEventRepository,
                            ResourceTransactionRepository resourceTransactionRepository) {
        this.gameRepository = gameRepository;
        this.gameBaseRepository = gameBaseRepository;
        this.gameAircraftRepository = gameAircraftRepository;
        this.gameMissionRepository = gameMissionRepository;
        this.baseStateRepository = baseStateRepository;
        this.aircraftStateRepository = aircraftStateRepository;
        this.gameEventRepository = gameEventRepository;
        this.resourceTransactionRepository = resourceTransactionRepository;
    }

    public GameStateDto getGameState(Long gameId) {
        Game game = gameRepository.findById(gameId).orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));

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
                            state.getAssignedMission() != null ? state.getAssignedMission().getCode() : null);
                })
                .toList();

        List<MissionStateDto> missions = gameMissionRepository.findByGame_IdOrderBySortOrder(gameId).stream()
                .map(m -> new MissionStateDto(m.getCode(), m.getMissionType().getCode(), m.getStatus().name(), m.getSortOrder()))
                .toList();

        GameSummaryDto summary = new GameSummaryDto(game.getId(), game.getName(), game.getScenario().getName(),
                game.getScenario().getVersion(), game.getStatus().name(), game.getCurrentRound());

        return new GameStateDto(summary, bases, aircraft, missions,
                gameEventRepository.findByGame_IdOrderByCreatedAtAsc(gameId).size(),
                resourceTransactionRepository.findByGame_Id(gameId).size());
    }
}
