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
import java.util.Locale;

@Service
/**
 * Creates persistent game instances from seeded scenario data.
 *
 * <p>This service materializes the scenario into live game tables so that all
 * later mutations operate on isolated per-game state.</p>
 */
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
        return createGameFromScenario(scenarioName, version, null, null, null);
    }

    @Transactional
    /**
     * Creates a new game from a named scenario version and populates all runtime
     * tables such as bases, aircraft, missions and their initial state rows.
     */
    public GameSummaryDto createGameFromScenario(String scenarioName, String version, String gameName) {
        return createGameFromScenario(scenarioName, version, gameName, null, null);
    }

    @Transactional
    public GameSummaryDto createGameFromScenario(String scenarioName,
                                                 String version,
                                                 String gameName,
                                                 Integer aircraftCount,
                                                 Map<String, Integer> missionTypeCounts) {
        String normalizedName = normalizeScenarioName(scenarioName);
        String normalizedVersion = normalizeScenarioVersion(version);

        Scenario scenario = scenarioRepository.findAll().stream()
                .filter(candidate -> candidate.getName() != null
                        && candidate.getVersion() != null
                        && candidate.getName().equalsIgnoreCase(normalizedName)
                        && normalizeScenarioVersion(candidate.getVersion()).equals(normalizedVersion))
                .findFirst()
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
        int requestedAircraftCount = aircraftCount != null ? aircraftCount : scenarioAircraft.size();
        if (requestedAircraftCount <= 0) {
            throw new IllegalArgumentException("Aircraft count must be at least 1");
        }
        int totalParkingCapacity = scenarioBases.stream().mapToInt(ScenarioBase::getParkingCapacity).sum();
        if (requestedAircraftCount > totalParkingCapacity) {
            throw new IllegalArgumentException("Aircraft count exceeds total parking capacity of " + totalParkingCapacity);
        }

        Map<String, Integer> occupiedStartSlots = new HashMap<>();
        for (int index = 0; index < requestedAircraftCount; index++) {
            ScenarioAircraft template = scenarioAircraft.get(index % scenarioAircraft.size());
            GameBase startBase = selectStartBase(template.getStartBaseCode(), scenarioBases, gameBaseByCode, occupiedStartSlots);
            GameAircraft ga = gameAircraftRepository.save(new GameAircraft(game, "F" + (index + 1), template.getAircraftType()));
            aircraftStateRepository.save(new AircraftState(ga, startBase,
                    template.getAircraftType().getMaxFuel(),
                    template.getAircraftType().getMaxWeapons(),
                    template.getAircraftType().getMaxFlightHours()));
            BaseState baseState = baseStateRepository.findByGameBase_Id(startBase.getId()).orElseThrow();
            baseState.setOccupiedParkingSlots(baseState.getOccupiedParkingSlots() + 1);
            baseStateRepository.save(baseState);
            occupiedStartSlots.merge(startBase.getCode(), 1, Integer::sum);
        }

        List<ScenarioMission> scenarioMissions = scenarioMissionRepository.findByScenario_IdOrderBySortOrder(scenario.getId());
        Map<String, Integer> requestedMissionCounts = normalizeMissionTypeCounts(missionTypeCounts, scenarioMissions);
        for (ScenarioMission template : scenarioMissions) {
            int count = requestedMissionCounts.getOrDefault(template.getMissionType().getCode(), 0);
            for (int index = 0; index < count; index++) {
                String missionCode = template.getMissionType().getCode() + "-" + (index + 1);
                gameMissionRepository.save(new GameMission(game, missionCode, template.getMissionType(), template.getSortOrder() * 100 + index));
            }
        }

        gameEventRepository.save(new GameEvent(game, null, null, null, null,
                se.smartairbase.mcpserver.domain.game.enums.EventType.MISSION_ASSIGNED,
                "Game created from scenario " + scenario.getName() + " v" + scenario.getVersion(),
                LocalDateTime.now()));

        return new GameSummaryDto(game.getId(), game.getName(), scenario.getName(), scenario.getVersion(),
                game.getStatus().name(), game.getCurrentRound(), null, false, true, false);
    }

    private String normalizeScenarioName(String scenarioName) {
        return scenarioName == null ? "" : scenarioName.trim();
    }

    private String normalizeScenarioVersion(String version) {
        if (version == null) {
            return "";
        }
        String trimmed = version.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        String upper = trimmed.toUpperCase(Locale.ROOT);
        return upper.startsWith("V") ? upper : "V" + upper;
    }

    private GameBase selectStartBase(String preferredBaseCode,
                                     List<ScenarioBase> scenarioBases,
                                     Map<String, GameBase> gameBaseByCode,
                                     Map<String, Integer> occupiedStartSlots) {
        if (preferredBaseCode != null) {
            ScenarioBase preferredBase = scenarioBases.stream()
                    .filter(base -> preferredBaseCode.equals(base.getCode()))
                    .findFirst()
                    .orElse(null);
            if (preferredBase != null && occupiedStartSlots.getOrDefault(preferredBaseCode, 0) < preferredBase.getParkingCapacity()) {
                return gameBaseByCode.get(preferredBaseCode);
            }
        }

        return scenarioBases.stream()
                .filter(base -> occupiedStartSlots.getOrDefault(base.getCode(), 0) < base.getParkingCapacity())
                .map(base -> gameBaseByCode.get(base.getCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No start base has free parking capacity"));
    }

    private Map<String, Integer> normalizeMissionTypeCounts(Map<String, Integer> missionTypeCounts, List<ScenarioMission> scenarioMissions) {
        Map<String, Integer> normalized = new HashMap<>();
        if (missionTypeCounts == null || missionTypeCounts.isEmpty()) {
            for (ScenarioMission scenarioMission : scenarioMissions) {
                normalized.put(scenarioMission.getMissionType().getCode(), 1);
            }
            return normalized;
        }

        for (ScenarioMission scenarioMission : scenarioMissions) {
            String missionTypeCode = scenarioMission.getMissionType().getCode();
            int count = missionTypeCounts.getOrDefault(missionTypeCode, 0);
            if (count < 0) {
                throw new IllegalArgumentException("Mission count for " + missionTypeCode + " must be zero or higher");
            }
            normalized.put(missionTypeCode, count);
        }
        return normalized;
    }
}
