package se.smartairbase.mcpserver.service;

import org.springframework.stereotype.Service;
import se.smartairbase.mcpserver.domain.game.Game;
import se.smartairbase.mcpserver.domain.game.GameAircraft;
import se.smartairbase.mcpserver.domain.game.GameAnalyticsSnapshot;
import se.smartairbase.mcpserver.domain.game.GameBase;
import se.smartairbase.mcpserver.domain.game.GameMission;
import se.smartairbase.mcpserver.domain.game.enums.GameStatus;
import se.smartairbase.mcpserver.domain.game.enums.MissionStatus;
import se.smartairbase.mcpserver.domain.rule.ScenarioBase;
import se.smartairbase.mcpserver.domain.rule.ScenarioSupplyRule;
import se.smartairbase.mcpserver.domain.rule.enums.ResourceType;
import se.smartairbase.mcpserver.repository.GameAircraftRepository;
import se.smartairbase.mcpserver.repository.GameAnalyticsSnapshotRepository;
import se.smartairbase.mcpserver.repository.GameBaseRepository;
import se.smartairbase.mcpserver.repository.GameMissionRepository;
import se.smartairbase.mcpserver.repository.ScenarioBaseRepository;
import se.smartairbase.mcpserver.repository.ScenarioSupplyRuleRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Builds one terminal analytics row per finished game so simulator statistics and later ML datasets can read
 * a stable set of pre-game features and end-of-game outcome metrics without replaying event history.
 */
@Service
public class GameAnalyticsSnapshotService {

    private final GameAnalyticsSnapshotRepository gameAnalyticsSnapshotRepository;
    private final GameBaseRepository gameBaseRepository;
    private final GameAircraftRepository gameAircraftRepository;
    private final GameMissionRepository gameMissionRepository;
    private final ScenarioBaseRepository scenarioBaseRepository;
    private final ScenarioSupplyRuleRepository scenarioSupplyRuleRepository;

    public GameAnalyticsSnapshotService(GameAnalyticsSnapshotRepository gameAnalyticsSnapshotRepository,
                                        GameBaseRepository gameBaseRepository,
                                        GameAircraftRepository gameAircraftRepository,
                                        GameMissionRepository gameMissionRepository,
                                        ScenarioBaseRepository scenarioBaseRepository,
                                        ScenarioSupplyRuleRepository scenarioSupplyRuleRepository) {
        this.gameAnalyticsSnapshotRepository = gameAnalyticsSnapshotRepository;
        this.gameBaseRepository = gameBaseRepository;
        this.gameAircraftRepository = gameAircraftRepository;
        this.gameMissionRepository = gameMissionRepository;
        this.scenarioBaseRepository = scenarioBaseRepository;
        this.scenarioSupplyRuleRepository = scenarioSupplyRuleRepository;
    }

    public void captureIfTerminal(Game game) {
        if (game == null || (game.getStatus() != GameStatus.WON && game.getStatus() != GameStatus.LOST)) {
            return;
        }
        if (gameAnalyticsSnapshotRepository.existsByGame_Id(game.getId())) {
            return;
        }

        List<GameBase> gameBases = gameBaseRepository.findByGame_Id(game.getId());
        List<GameAircraft> aircraft = gameAircraftRepository.findByGame_Id(game.getId());
        List<GameMission> missions = gameMissionRepository.findByGame_IdOrderBySortOrder(game.getId());
        List<ScenarioBase> scenarioBases = scenarioBaseRepository.findByScenario_Id(game.getScenario().getId());

        int m1Count = (int) missions.stream().filter(mission -> "M1".equals(mission.getMissionType().getCode())).count();
        int m2Count = (int) missions.stream().filter(mission -> "M2".equals(mission.getMissionType().getCode())).count();
        int m3Count = (int) missions.stream().filter(mission -> "M3".equals(mission.getMissionType().getCode())).count();
        int completedMissionCount = (int) missions.stream()
                .filter(mission -> mission.getStatus() == MissionStatus.COMPLETED)
                .count();
        int destroyedAircraftCount = (int) aircraft.stream()
                .filter(item -> item.getStatus() != null)
                .filter(item -> "DESTROYED".equals(item.getStatus().name()) || "CRASHED".equals(item.getStatus().name()))
                .count();
        int survivingAircraftCount = aircraft.size() - destroyedAircraftCount;

        int totalParkingCapacity = gameBases.stream().mapToInt(base -> safe(base.getParkingCapacity())).sum();
        int totalMaintenanceCapacity = gameBases.stream().mapToInt(base -> safe(base.getMaintenanceCapacity())).sum();
        int totalFuelMax = gameBases.stream().mapToInt(base -> safe(base.getFuelMax())).sum();
        int totalWeaponsMax = gameBases.stream().mapToInt(base -> safe(base.getWeaponsMax())).sum();
        int totalSparePartsMax = gameBases.stream().mapToInt(base -> safe(base.getSparePartsMax())).sum();

        int totalFuelStart = scenarioBases.stream().mapToInt(base -> safe(base.getFuelStart())).sum();
        int totalWeaponsStart = scenarioBases.stream().mapToInt(base -> safe(base.getWeaponsStart())).sum();
        int totalSparePartsStart = scenarioBases.stream().mapToInt(base -> safe(base.getSparePartsStart())).sum();

        int fuelDeliveryAmountTotal = 0;
        int weaponsDeliveryAmountTotal = 0;
        int sparePartsDeliveryAmountTotal = 0;
        for (ScenarioBase scenarioBase : scenarioBases) {
            for (ScenarioSupplyRule rule : scenarioSupplyRuleRepository.findByScenarioBase_Id(scenarioBase.getId())) {
                if (rule.getResource() == ResourceType.FUEL) {
                    fuelDeliveryAmountTotal += safe(rule.getDeliveryAmount());
                } else if (rule.getResource() == ResourceType.WEAPONS) {
                    weaponsDeliveryAmountTotal += safe(rule.getDeliveryAmount());
                } else if (rule.getResource() == ResourceType.SPARE_PARTS) {
                    sparePartsDeliveryAmountTotal += safe(rule.getDeliveryAmount());
                }
            }
        }

        gameAnalyticsSnapshotRepository.save(new GameAnalyticsSnapshot(
                game,
                game.getScenario().getName(),
                game.getStatus().name(),
                game.getStatus() == GameStatus.WON,
                safe(game.getCurrentRound()),
                game.getDiceSelectionProfile() != null ? game.getDiceSelectionProfile().name() : null,
                aircraft.size(),
                survivingAircraftCount,
                destroyedAircraftCount,
                missions.size(),
                completedMissionCount,
                m1Count,
                m2Count,
                m3Count,
                totalParkingCapacity,
                totalMaintenanceCapacity,
                totalFuelStart,
                totalFuelMax,
                totalWeaponsStart,
                totalWeaponsMax,
                totalSparePartsStart,
                totalSparePartsMax,
                fuelDeliveryAmountTotal,
                weaponsDeliveryAmountTotal,
                sparePartsDeliveryAmountTotal,
                LocalDateTime.now()
        ));
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }
}
