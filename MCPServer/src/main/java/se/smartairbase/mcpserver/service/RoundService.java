package se.smartairbase.mcpserver.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import se.smartairbase.mcpserver.domain.game.*;
import se.smartairbase.mcpserver.domain.game.enums.AircraftStatus;
import se.smartairbase.mcpserver.domain.game.enums.EventType;
import se.smartairbase.mcpserver.domain.game.enums.MissionStatus;
import se.smartairbase.mcpserver.domain.rule.RepairRule;
import se.smartairbase.mcpserver.domain.rule.ScenarioBase;
import se.smartairbase.mcpserver.domain.rule.ScenarioSupplyRule;
import se.smartairbase.mcpserver.domain.rule.enums.DamageType;
import se.smartairbase.mcpserver.domain.rule.enums.ResourceType;
import se.smartairbase.mcpserver.mcp.dto.ActionResultDto;
import se.smartairbase.mcpserver.mcp.dto.RoundExecutionResultDto;
import se.smartairbase.mcpserver.repository.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RoundService {

    private final GameRepository gameRepository;
    private final GameAircraftRepository gameAircraftRepository;
    private final GameMissionRepository gameMissionRepository;
    private final GameBaseRepository gameBaseRepository;
    private final AircraftStateRepository aircraftStateRepository;
    private final BaseStateRepository baseStateRepository;
    private final GameRoundRepository gameRoundRepository;
    private final DiceRollRepository diceRollRepository;
    private final RepairRuleRepository repairRuleRepository;
    private final GameEventRepository gameEventRepository;
    private final ResourceTransactionRepository resourceTransactionRepository;
    private final ScenarioBaseRepository scenarioBaseRepository;
    private final ScenarioSupplyRuleRepository scenarioSupplyRuleRepository;

    public RoundService(GameRepository gameRepository,
                        GameAircraftRepository gameAircraftRepository,
                        GameMissionRepository gameMissionRepository,
                        GameBaseRepository gameBaseRepository,
                        AircraftStateRepository aircraftStateRepository,
                        BaseStateRepository baseStateRepository,
                        GameRoundRepository gameRoundRepository,
                        DiceRollRepository diceRollRepository,
                        RepairRuleRepository repairRuleRepository,
                        GameEventRepository gameEventRepository,
                        ResourceTransactionRepository resourceTransactionRepository,
                        ScenarioBaseRepository scenarioBaseRepository,
                        ScenarioSupplyRuleRepository scenarioSupplyRuleRepository) {
        this.gameRepository = gameRepository;
        this.gameAircraftRepository = gameAircraftRepository;
        this.gameMissionRepository = gameMissionRepository;
        this.gameBaseRepository = gameBaseRepository;
        this.aircraftStateRepository = aircraftStateRepository;
        this.baseStateRepository = baseStateRepository;
        this.gameRoundRepository = gameRoundRepository;
        this.diceRollRepository = diceRollRepository;
        this.repairRuleRepository = repairRuleRepository;
        this.gameEventRepository = gameEventRepository;
        this.resourceTransactionRepository = resourceTransactionRepository;
        this.scenarioBaseRepository = scenarioBaseRepository;
        this.scenarioSupplyRuleRepository = scenarioSupplyRuleRepository;
    }

    @Transactional
    public ActionResultDto assignMission(Long gameId, String aircraftCode, String missionCode) {
        Game game = gameRepository.findById(gameId).orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));
        GameAircraft aircraft = gameAircraftRepository.findByGame_IdAndCode(gameId, aircraftCode)
                .orElseThrow(() -> new IllegalArgumentException("Aircraft not found: " + aircraftCode));
        GameMission mission = gameMissionRepository.findByGame_IdAndCode(gameId, missionCode)
                .orElseThrow(() -> new IllegalArgumentException("Mission not found: " + missionCode));
        AircraftState aircraftState = aircraftStateRepository.findByGameAircraft_Id(aircraft.getId()).orElseThrow();

        if (mission.getStatus() != MissionStatus.AVAILABLE) {
            return new ActionResultDto(false, "Mission is not available");
        }
        if (aircraft.getStatus() != AircraftStatus.READY) {
            return new ActionResultDto(false, "Aircraft is not ready");
        }
        if (aircraftState.getDamage() == DamageType.FULL_SERVICE_REQUIRED) {
            return new ActionResultDto(false, "Aircraft requires full service");
        }
        if (aircraftState.getFuel() < mission.getMissionType().getFuelCost()) {
            return new ActionResultDto(false, "Aircraft lacks fuel for mission");
        }
        if (aircraftState.getWeapons() < mission.getMissionType().getWeaponCost()) {
            return new ActionResultDto(false, "Aircraft lacks weapons for mission");
        }
        if (aircraftState.getRemainingFlightHours() < mission.getMissionType().getFlightTimeCost()) {
            return new ActionResultDto(false, "Aircraft lacks remaining flight hours");
        }

        aircraftState.assignMission(mission);
        aircraft.setStatus(AircraftStatus.ON_MISSION);
        mission.setStatus(MissionStatus.ASSIGNED);
        gameEventRepository.save(new GameEvent(game, null, aircraft, aircraftState.getCurrentBase(), mission,
                EventType.MISSION_ASSIGNED,
                aircraftCode + " assigned to " + missionCode,
                LocalDateTime.now()));
        return new ActionResultDto(true, aircraftCode + " assigned to " + missionCode);
    }

    @Transactional
    public RoundExecutionResultDto executeRound(Long gameId) {
        Game game = gameRepository.findById(gameId).orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));
        int nextRound = game.getCurrentRound() + 1;
        GameRound round = gameRoundRepository.save(new GameRound(game, nextRound, LocalDateTime.now()));
        game.setCurrentRound(nextRound);

        List<String> aircraftUpdates = new ArrayList<>();
        List<String> completedMissions = new ArrayList<>();
        List<String> supplyDeliveries = new ArrayList<>();

        processMaintenanceProgress(game, round, aircraftUpdates);
        processAssignedMissions(game, round, aircraftUpdates, completedMissions);
        applySupplyDeliveries(game, round, supplyDeliveries);
        updateGameOutcome(game);

        round.end(LocalDateTime.now());
        return new RoundExecutionResultDto(game.getId(), nextRound, game.getStatus().name(), completedMissions, aircraftUpdates, supplyDeliveries);
    }

    private void processMaintenanceProgress(Game game, GameRound round, List<String> aircraftUpdates) {
        for (GameAircraft aircraft : gameAircraftRepository.findByGame_Id(game.getId())) {
            AircraftState state = aircraftStateRepository.findByGameAircraft_Id(aircraft.getId()).orElseThrow();
            if (aircraft.getStatus() == AircraftStatus.IN_MAINTENANCE && state.getRepairRoundsRemaining() > 0) {
                state.setRepairRoundsRemaining(state.getRepairRoundsRemaining() - 1);
                aircraftUpdates.add(aircraft.getCode() + " maintenance progress, remaining=" + state.getRepairRoundsRemaining());
                if (state.getRepairRoundsRemaining() == 0) {
                    state.setDamage(DamageType.NONE);
                    aircraft.setStatus(AircraftStatus.READY);
                    BaseState baseState = baseStateRepository.findByGameBase_Id(state.getCurrentBase().getId()).orElseThrow();
                    baseState.setOccupiedMaintSlots(Math.max(0, baseState.getOccupiedMaintSlots() - 1));
                    gameEventRepository.save(new GameEvent(game, round, aircraft, state.getCurrentBase(), null,
                            EventType.REPAIR_COMPLETE, aircraft.getCode() + " repair completed", LocalDateTime.now()));
                }
            }
        }
    }

    private void processAssignedMissions(Game game, GameRound round, List<String> aircraftUpdates, List<String> completedMissions) {
        for (GameAircraft aircraft : gameAircraftRepository.findByGame_Id(game.getId())) {
            AircraftState state = aircraftStateRepository.findByGameAircraft_Id(aircraft.getId()).orElseThrow();
            GameMission mission = state.getAssignedMission();
            if (mission == null) {
                continue;
            }

            aircraft.setStatus(AircraftStatus.PARKED);
            mission.setStatus(MissionStatus.COMPLETED);
            state.applyMissionCosts(mission.getMissionType().getFuelCost(), mission.getMissionType().getWeaponCost(), mission.getMissionType().getFlightTimeCost());
            state.clearAssignedMission();
            completedMissions.add(mission.getCode());
            aircraftUpdates.add(aircraft.getCode() + " completed " + mission.getCode());

            resourceTransactionRepository.save(new ResourceTransaction(game, round, null, aircraft, ResourceType.FUEL,
                    -mission.getMissionType().getFuelCost(), "MISSION_COST", LocalDateTime.now()));
            if (mission.getMissionType().getWeaponCost() > 0) {
                resourceTransactionRepository.save(new ResourceTransaction(game, round, null, aircraft, ResourceType.WEAPONS,
                        -mission.getMissionType().getWeaponCost(), "MISSION_COST", LocalDateTime.now()));
            }

            gameEventRepository.save(new GameEvent(game, round, aircraft, state.getCurrentBase(), mission,
                    EventType.MISSION_COMPLETED, aircraft.getCode() + " completed " + mission.getCode(), LocalDateTime.now()));

            int diceValue = ThreadLocalRandom.current().nextInt(1, 7);
            RepairRule repairRule = repairRuleRepository.findByDiceValue(diceValue).orElseThrow();
            diceRollRepository.save(new DiceRoll(round, aircraft, diceValue, repairRule, LocalDateTime.now()));
            gameEventRepository.save(new GameEvent(game, round, aircraft, state.getCurrentBase(), mission,
                    EventType.DICE_ROLLED, aircraft.getCode() + " rolled " + diceValue, LocalDateTime.now()));

            if (repairRule.getDamage() != DamageType.NONE) {
                state.setDamage(repairRule.getDamage());
                state.setRepairRoundsRemaining(repairRule.getRepairRounds());
                BaseState baseState = state.getCurrentBase() != null ? baseStateRepository.findByGameBase_Id(state.getCurrentBase().getId()).orElseThrow() : null;
                if (baseState != null && state.getCurrentBase().getMaintenanceCapacity() > baseState.getOccupiedMaintSlots()
                        && baseState.getSparePartsStock() >= repairRule.getSparePartsCost()) {
                    baseState.consumeSpareParts(repairRule.getSparePartsCost());
                    baseState.setOccupiedMaintSlots(baseState.getOccupiedMaintSlots() + 1);
                    aircraft.setStatus(AircraftStatus.IN_MAINTENANCE);
                    resourceTransactionRepository.save(new ResourceTransaction(game, round, state.getCurrentBase(), aircraft,
                            ResourceType.SPARE_PARTS, -repairRule.getSparePartsCost(), "REPAIR", LocalDateTime.now()));
                    aircraftUpdates.add(aircraft.getCode() + " entered maintenance for " + repairRule.getDamage().name());
                } else {
                    aircraft.setStatus(AircraftStatus.WAITING_MAINTENANCE);
                    aircraftUpdates.add(aircraft.getCode() + " waiting for maintenance slot or spare parts");
                }
            } else {
                aircraft.setStatus(AircraftStatus.READY);
            }
        }
    }

    private void applySupplyDeliveries(Game game, GameRound round, List<String> supplyDeliveries) {
        List<GameBase> bases = gameBaseRepository.findByGame_Id(game.getId());
        for (GameBase gameBase : bases) {
            BaseState baseState = baseStateRepository.findByGameBase_Id(gameBase.getId()).orElseThrow();
            ScenarioBase scenarioBase = scenarioBaseRepository.findByScenario_IdAndCode(game.getScenario().getId(), gameBase.getCode()).orElseThrow();
            for (ScenarioSupplyRule rule : scenarioSupplyRuleRepository.findByScenarioBase_Id(scenarioBase.getId())) {
                if (round.getRoundNumber() % rule.getFrequencyRounds() != 0) {
                    continue;
                }
                switch (rule.getResource()) {
                    case FUEL -> baseState.addFuel(rule.getDeliveryAmount(), gameBase.getFuelMax());
                    case WEAPONS -> baseState.addWeapons(rule.getDeliveryAmount(), gameBase.getWeaponsMax());
                    case SPARE_PARTS -> baseState.addSpareParts(rule.getDeliveryAmount(), gameBase.getSparePartsMax());
                }
                resourceTransactionRepository.save(new ResourceTransaction(game, round, gameBase, null,
                        rule.getResource(), rule.getDeliveryAmount(), "SUPPLY_DELIVERY", LocalDateTime.now()));
                String text = gameBase.getCode() + " received " + rule.getDeliveryAmount() + " " + rule.getResource();
                supplyDeliveries.add(text);
                gameEventRepository.save(new GameEvent(game, round, null, gameBase, null,
                        EventType.SUPPLY_DELIVERY, text, LocalDateTime.now()));
            }
        }
    }

    private void updateGameOutcome(Game game) {
        List<GameMission> missions = gameMissionRepository.findByGame_IdOrderBySortOrder(game.getId());
        boolean allCompleted = missions.stream().allMatch(m -> m.getStatus() == MissionStatus.COMPLETED);
        if (allCompleted) {
            game.markWon(LocalDateTime.now());
        }
    }
}
