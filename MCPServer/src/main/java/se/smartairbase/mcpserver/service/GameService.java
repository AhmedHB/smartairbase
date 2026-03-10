package se.smartairbase.mcpserver.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import se.smartairbase.mcpserver.domain.game.*;
import se.smartairbase.mcpserver.domain.rule.*;
import se.smartairbase.mcpserver.mcp.dto.GameSummaryDto;
import se.smartairbase.mcpserver.repository.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GameService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioBaseRepository scenarioBaseRepository;
    private final ScenarioAircraftRepository scenarioAircraftRepository;
    private final ScenarioMissionRepository scenarioMissionRepository;
    private final GameRepository gameRepository;
    private final GameBaseRepository gameBaseRepository;
    private final GameAircraftRepository gameAircraftRepository;
    private final GameMissionRepository gameMissionRepository;
    private final BaseStateRepository baseStateRepository;
    private final AircraftStateRepository aircraftStateRepository;
    private final GameEventRepository gameEventRepository;

    public GameService(ScenarioRepository scenarioRepository,
                       ScenarioBaseRepository scenarioBaseRepository,
                       ScenarioAircraftRepository scenarioAircraftRepository,
                       ScenarioMissionRepository scenarioMissionRepository,
                       GameRepository gameRepository,
                       GameBaseRepository gameBaseRepository,
                       GameAircraftRepository gameAircraftRepository,
                       GameMissionRepository gameMissionRepository,
                       BaseStateRepository baseStateRepository,
                       AircraftStateRepository aircraftStateRepository,
                       GameEventRepository gameEventRepository) {
        this.scenarioRepository = scenarioRepository;
        this.scenarioBaseRepository = scenarioBaseRepository;
        this.scenarioAircraftRepository = scenarioAircraftRepository;
        this.scenarioMissionRepository = scenarioMissionRepository;
        this.gameRepository = gameRepository;
        this.gameBaseRepository = gameBaseRepository;
        this.gameAircraftRepository = gameAircraftRepository;
        this.gameMissionRepository = gameMissionRepository;
        this.baseStateRepository = baseStateRepository;
        this.aircraftStateRepository = aircraftStateRepository;
        this.gameEventRepository = gameEventRepository;
    }

    @Transactional
    public GameSummaryDto createGameFromScenario(String scenarioName, String version) {
        return createGameFromScenario(scenarioName, version, null);
    }

    @Transactional
    public GameSummaryDto createGameFromScenario(String scenarioName, String version, String gameName) {
        Scenario scenario = scenarioRepository.findByNameAndVersion(scenarioName, version)
                .orElseThrow(() -> new IllegalArgumentException("Scenario not found: " + scenarioName + " v" + version));

        Game game = new Game(scenario, gameName == null || gameName.isBlank() ? scenarioName + " " + version : gameName);
        game.markActive(LocalDateTime.now());
        game = gameRepository.save(game);

        List<ScenarioBase> scenarioBases = scenarioBaseRepository.findByScenario_Id(scenario.getId());
        Map<String, GameBase> gameBaseByCode = new HashMap<>();
        for (ScenarioBase sb : scenarioBases) {
            GameBase gb = new GameBase(game, sb.getCode(), sb.getName(), sb.getBaseType(), sb.getParkingCapacity(),
                    sb.getMaintenanceCapacity(), sb.getFuelMax(), sb.getWeaponsMax(), sb.getSparePartsMax());
            gb = gameBaseRepository.save(gb);
            gameBaseByCode.put(gb.getCode(), gb);
            baseStateRepository.save(new BaseState(gb, sb.getFuelStart(), sb.getWeaponsStart(), sb.getSparePartsStart(), 0, 0));
        }

        List<ScenarioAircraft> scenarioAircraft = scenarioAircraftRepository.findByScenario_Id(scenario.getId());
        for (ScenarioAircraft sa : scenarioAircraft) {
            GameBase startBase = gameBaseByCode.get(sa.getStartBaseCode());
            if (startBase == null) {
                throw new IllegalStateException("Missing start base: " + sa.getStartBaseCode());
            }
            GameAircraft ga = gameAircraftRepository.save(new GameAircraft(game, sa.getCode(), sa.getAircraftType()));
            aircraftStateRepository.save(new AircraftState(ga, startBase,
                    sa.getAircraftType().getMaxFuel(),
                    sa.getAircraftType().getMaxWeapons(),
                    sa.getAircraftType().getMaxFlightHours()));
            BaseState baseState = baseStateRepository.findByGameBase_Id(startBase.getId()).orElseThrow();
            baseState.setOccupiedParkingSlots(baseState.getOccupiedParkingSlots() + 1);
            baseStateRepository.save(baseState);
        }

        List<ScenarioMission> scenarioMissions = scenarioMissionRepository.findByScenario_IdOrderBySortOrder(scenario.getId());
        for (ScenarioMission sm : scenarioMissions) {
            gameMissionRepository.save(new GameMission(game, sm.getCode(), sm.getMissionType(), sm.getSortOrder()));
        }

        gameEventRepository.save(new GameEvent(game, null, null, null, null,
                se.smartairbase.mcpserver.domain.game.enums.EventType.MISSION_ASSIGNED,
                "Game created from scenario " + scenario.getName() + " v" + scenario.getVersion(),
                LocalDateTime.now()));

        return new GameSummaryDto(game.getId(), game.getName(), scenario.getName(), scenario.getVersion(), game.getStatus().name(), game.getCurrentRound());
    }
}
