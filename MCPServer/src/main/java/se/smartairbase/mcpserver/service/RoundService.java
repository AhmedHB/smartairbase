package se.smartairbase.mcpserver.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import se.smartairbase.mcpserver.domain.game.AircraftState;
import se.smartairbase.mcpserver.domain.game.BaseState;
import se.smartairbase.mcpserver.domain.game.DiceRoll;
import se.smartairbase.mcpserver.domain.game.Game;
import se.smartairbase.mcpserver.domain.game.GameAircraft;
import se.smartairbase.mcpserver.domain.game.GameBase;
import se.smartairbase.mcpserver.domain.game.GameEvent;
import se.smartairbase.mcpserver.domain.game.GameMission;
import se.smartairbase.mcpserver.domain.game.GameRound;
import se.smartairbase.mcpserver.domain.game.ResourceTransaction;
import se.smartairbase.mcpserver.domain.game.enums.AircraftStatus;
import se.smartairbase.mcpserver.domain.game.enums.DiceSelectionMode;
import se.smartairbase.mcpserver.domain.game.enums.DiceSelectionProfile;
import se.smartairbase.mcpserver.domain.game.enums.EventType;
import se.smartairbase.mcpserver.domain.game.enums.GameStatus;
import se.smartairbase.mcpserver.domain.game.enums.MissionStatus;
import se.smartairbase.mcpserver.domain.game.enums.RoundPhase;
import se.smartairbase.mcpserver.domain.rule.RepairRule;
import se.smartairbase.mcpserver.domain.rule.ScenarioBase;
import se.smartairbase.mcpserver.domain.rule.ScenarioSupplyRule;
import se.smartairbase.mcpserver.domain.rule.enums.BaseServiceType;
import se.smartairbase.mcpserver.domain.rule.enums.DamageType;
import se.smartairbase.mcpserver.domain.rule.enums.ResourceType;
import se.smartairbase.mcpserver.mcp.dto.ActionResultDto;
import se.smartairbase.mcpserver.mcp.dto.LandingOptionDto;
import se.smartairbase.mcpserver.mcp.dto.LandingOptionsDto;
import se.smartairbase.mcpserver.mcp.dto.RoundExecutionResultDto;
import se.smartairbase.mcpserver.repository.AircraftStateRepository;
import se.smartairbase.mcpserver.repository.BaseStateRepository;
import se.smartairbase.mcpserver.repository.BaseTypeServiceRepository;
import se.smartairbase.mcpserver.repository.DiceRollRepository;
import se.smartairbase.mcpserver.repository.GameAircraftRepository;
import se.smartairbase.mcpserver.repository.GameBaseRepository;
import se.smartairbase.mcpserver.repository.GameEventRepository;
import se.smartairbase.mcpserver.repository.GameMissionRepository;
import se.smartairbase.mcpserver.repository.GameRepository;
import se.smartairbase.mcpserver.repository.GameRoundRepository;
import se.smartairbase.mcpserver.repository.RepairRuleRepository;
import se.smartairbase.mcpserver.repository.ResourceTransactionRepository;
import se.smartairbase.mcpserver.repository.ScenarioBaseRepository;
import se.smartairbase.mcpserver.repository.ScenarioSupplyRuleRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
/**
 * Central game engine for the stepwise round flow.
 *
 * <p>This service enforces the round state machine and owns the most important
 * transitions in the game:
 * planning, mission resolution, dice recording, landing, holding and
 * round completion.</p>
 *
 * <p>The MCP client orchestrates which tool to call next, but this service
 * validates that each transition is legal before state is persisted.</p>
 */
public class RoundService {

    private static final int HOLDING_FUEL_COST = 5;

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
    private final BaseTypeServiceRepository baseTypeServiceRepository;

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
                        ScenarioSupplyRuleRepository scenarioSupplyRuleRepository,
                        BaseTypeServiceRepository baseTypeServiceRepository) {
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
        this.baseTypeServiceRepository = baseTypeServiceRepository;
    }

    @Transactional
    /**
     * Opens a new round in the planning phase.
     *
     * <p>This method is the only valid entry point for a new round. It fails if
     * the game is not active or if another round is already open.</p>
     */
    public RoundExecutionResultDto startRound(Long gameId) {
        Game game = loadGame(gameId);
        requireGameActive(game);
        if (gameRoundRepository.findFirstByGame_IdAndEndedAtIsNullOrderByRoundNumberDesc(gameId).isPresent()) {
            throw new IllegalStateException("A round is already in progress");
        }

        int nextRound = game.getCurrentRound() + 1;
        GameRound round = gameRoundRepository.save(new GameRound(game, nextRound, RoundPhase.PLANNING, LocalDateTime.now()));
        game.setCurrentRound(nextRound);
        logEvent(game, round, null, null, null, EventType.TAKEOFF, "Round " + nextRound + " started");
        return buildRoundResult(game, round, List.of("Round " + nextRound + " started"), List.of(), List.of(), List.of(), List.of());
    }

    @Transactional
    /**
     * Assigns a mission to an aircraft during the planning phase.
     *
     * <p>The method validates readiness, resources, maintenance requirements and
     * phase restrictions before moving the aircraft from a parking slot into
     * mission state.</p>
     */
    public ActionResultDto assignMission(Long gameId, String aircraftCode, String missionCode) {
        Game game = loadGame(gameId);
        GameRound round = requireActiveRound(gameId, RoundPhase.PLANNING);
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
        if (aircraftState.getCurrentBase() == null) {
            return new ActionResultDto(false, "Aircraft is not parked at a base");
        }
        if (aircraftState.isInHolding()) {
            return new ActionResultDto(false, "Aircraft in holding cannot take a mission");
        }
        if (aircraftState.getDamage() == DamageType.FULL_SERVICE_REQUIRED || aircraftState.getRemainingFlightHours() == 0) {
            return new ActionResultDto(false, "Aircraft requires full service");
        }
        if (aircraftState.getFuel() < mission.getFuelCost()) {
            return new ActionResultDto(false, "Aircraft lacks fuel for mission");
        }
        if (aircraftState.getWeapons() < mission.getWeaponCost()) {
            return new ActionResultDto(false, "Aircraft lacks weapons for mission");
        }
        if (aircraftState.getRemainingFlightHours() < mission.getFlightTimeCost()) {
            return new ActionResultDto(false, "Aircraft lacks remaining flight hours");
        }

        BaseState originBaseState = baseStateRepository.findByGameBase_Id(aircraftState.getCurrentBase().getId()).orElseThrow();
        originBaseState.decrementOccupiedParkingSlots();
        aircraftState.assignMission(mission);
        aircraft.setStatus(AircraftStatus.ON_MISSION);
        mission.setStatus(MissionStatus.ASSIGNED);
        logEvent(game, round, aircraft, aircraftState.getCurrentBase(), mission, EventType.MISSION_ASSIGNED,
                aircraftCode + " assigned to " + missionCode);
        return new ActionResultDto(true, aircraftCode + " assigned to " + missionCode);
    }

    @Transactional
    /**
     * Resolves all missions assigned in the current planning phase.
     *
     * <p>Mission cost is applied immediately and every aircraft that completed a
     * mission is moved to {@code AWAITING_DICE_ROLL}. Damage is intentionally not
     * resolved here because the player provides the dice result in a later step.</p>
     */
    public RoundExecutionResultDto resolveMissions(Long gameId) {
        Game game = loadGame(gameId);
        GameRound round = requireActiveRound(gameId, RoundPhase.PLANNING);

        List<String> aircraftUpdates = new ArrayList<>();
        List<String> completedMissions = new ArrayList<>();
        for (GameAircraft aircraft : gameAircraftRepository.findByGame_Id(gameId)) {
            AircraftState state = aircraftStateRepository.findByGameAircraft_Id(aircraft.getId()).orElseThrow();
            GameMission mission = state.getAssignedMission();
            if (mission == null) {
                continue;
            }

            mission.setStatus(MissionStatus.COMPLETED);
            state.applyMissionCosts(mission.getFuelCost(), mission.getWeaponCost(), mission.getFlightTimeCost());
            state.clearAssignedMission();
            state.setCurrentBase(null);
            aircraft.setStatus(AircraftStatus.AWAITING_DICE_ROLL);
            completedMissions.add(mission.getCode());
            aircraftUpdates.add(aircraft.getCode() + " completed " + mission.getCode());

            resourceTransactionRepository.save(new ResourceTransaction(game, round, null, aircraft, ResourceType.FUEL,
                    -mission.getFuelCost(), "MISSION_COST", LocalDateTime.now()));
            if (mission.getWeaponCost() > 0) {
                resourceTransactionRepository.save(new ResourceTransaction(game, round, null, aircraft, ResourceType.WEAPONS,
                        -mission.getWeaponCost(), "MISSION_COST", LocalDateTime.now()));
            }

            logEvent(game, round, aircraft, null, mission, EventType.MISSION_COMPLETED,
                    aircraft.getCode() + " completed " + mission.getCode());
        }

        if (allAircraftResolvedForDice(gameId)) {
            round.setPhase(RoundPhase.LANDING);
        }
        else {
            round.setPhase(RoundPhase.DICE_ROLL);
        }
        updateGameOutcome(game);
        return buildRoundResult(game, round, List.of("Mission resolution completed"), pendingAircraft(gameId),
                aircraftUpdates, completedMissions, List.of());
    }

    @Transactional
    /**
     * Records the player's dice result for one aircraft.
     *
     * <p>This is the transition from the dice phase into either landing
     * preparation or immediate aircraft loss. The selected outcome rule is
     * persisted so later maintenance and audit decisions remain deterministic.
     * The method also stores how the roll was chosen and refreshes the
     * aggregated game-level dice selection profile.</p>
     */
    public ActionResultDto recordDiceRoll(Long gameId, String aircraftCode, Integer diceValue, String diceSelectionModeValue) {
        Game game = loadGame(gameId);
        GameRound round = requireActiveRound(gameId, RoundPhase.DICE_ROLL);
        GameAircraft aircraft = gameAircraftRepository.findByGame_IdAndCode(gameId, aircraftCode)
                .orElseThrow(() -> new IllegalArgumentException("Aircraft not found: " + aircraftCode));
        AircraftState state = aircraftStateRepository.findByGameAircraft_Id(aircraft.getId()).orElseThrow();
        DiceSelectionMode diceSelectionMode = parseDiceSelectionMode(diceSelectionModeValue);

        if (aircraft.getStatus() != AircraftStatus.AWAITING_DICE_ROLL) {
            return new ActionResultDto(false, "Aircraft is not waiting for a dice roll");
        }
        if (diceValue == null || diceValue < 1 || diceValue > 6) {
            return new ActionResultDto(false, "Dice value must be between 1 and 6");
        }
        if (diceRollRepository.findByGameRound_IdAndGameAircraft_Id(round.getId(), aircraft.getId()).isPresent()) {
            return new ActionResultDto(false, "Dice already recorded for aircraft");
        }

        RepairRule repairRule = repairRuleRepository.findByDiceValue(diceValue).orElseThrow();
        if (repairRule.getDamage() != DamageType.DESTROYED
                && state.getRemainingFlightHours() == 0
                && !repairRule.isRequiresFullService()) {
            repairRule = repairRuleRepository.findByRequiresFullServiceTrue()
                    .orElseThrow(() -> new IllegalStateException("Full service rule not configured"));
        }

        state.setLastDiceValue(diceValue);
        state.setDamage(repairRule.getDamage());
        state.setRepairRoundsRemaining(repairRule.getRepairRounds());
        state.setInHolding(false);
        if (repairRule.getDamage() == DamageType.DESTROYED) {
            state.setCurrentBase(null);
            aircraft.setStatus(AircraftStatus.DESTROYED);
        }
        else {
            aircraft.setStatus(AircraftStatus.AWAITING_LANDING);
        }

        diceRollRepository.save(new DiceRoll(round, aircraft, diceValue, repairRule, diceSelectionMode, LocalDateTime.now()));
        updateGameDiceSelectionProfile(game);
        logEvent(game, round, aircraft, null, null, EventType.DICE_ROLLED,
                aircraft.getCode() + " rolled " + diceValue + " -> " + repairRule.getDamage().name());

        if (allAircraftResolvedForDice(gameId)) {
            round.setPhase(RoundPhase.LANDING);
        }

        return new ActionResultDto(true, aircraftCode + " dice roll recorded");
    }

    private DiceSelectionMode parseDiceSelectionMode(String value) {
        try {
            return DiceSelectionMode.valueOf(value == null ? "" : value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown dice selection mode: " + value);
        }
    }

    private void updateGameDiceSelectionProfile(Game game) {
        List<DiceSelectionMode> modes = diceRollRepository.findByGameRound_Game_IdOrderByRolledAtAsc(game.getId()).stream()
                .map(DiceRoll::getDiceSelectionMode)
                .toList();
        if (modes.isEmpty()) {
            game.setDiceSelectionProfile(null);
            return;
        }

        boolean allManual = modes.stream().allMatch(mode -> mode == DiceSelectionMode.MANUAL_DIRECT_SELECTION || mode == DiceSelectionMode.MANUAL_RANDOM_SELECTION);
        boolean allAuto = modes.stream().allMatch(mode -> mode == DiceSelectionMode.AUTO_RANDOM || mode == DiceSelectionMode.AUTO_MIN_DAMAGE || mode == DiceSelectionMode.AUTO_MAX_DAMAGE);
        long distinctModeCount = modes.stream().distinct().count();

        if (allManual) {
            if (distinctModeCount == 1) {
                game.setDiceSelectionProfile(DiceSelectionProfile.valueOf(modes.getFirst().name()));
            }
            else {
                game.setDiceSelectionProfile(DiceSelectionProfile.MANUAL_MIXED);
            }
            return;
        }
        if (allAuto && distinctModeCount == 1) {
            game.setDiceSelectionProfile(DiceSelectionProfile.valueOf(modes.getFirst().name()));
            return;
        }
        game.setDiceSelectionProfile(DiceSelectionProfile.MIXED);
    }

    @Transactional
    /**
     * Lists valid landing targets for an aircraft that already has a resolved
     * damage outcome.
     *
     * <p>The result is intended for the MCP client so it can guide the player
     * without reimplementing capacity and service rules on the client side.
     * Aircraft destroyed by the dice outcome never reach this step.</p>
     */
    public LandingOptionsDto listAvailableLandingBases(Long gameId, String aircraftCode) {
        requireActiveRound(gameId, RoundPhase.LANDING);
        GameAircraft aircraft = gameAircraftRepository.findByGame_IdAndCode(gameId, aircraftCode)
                .orElseThrow(() -> new IllegalArgumentException("Aircraft not found: " + aircraftCode));
        AircraftState state = aircraftStateRepository.findByGameAircraft_Id(aircraft.getId()).orElseThrow();
        if (aircraft.getStatus() != AircraftStatus.AWAITING_LANDING) {
            throw new IllegalStateException("Aircraft is not awaiting landing");
        }

        List<LandingOptionDto> options = gameBaseRepository.findByGame_Id(gameId).stream()
                .sorted(Comparator.comparing(GameBase::getCode))
                .map(base -> toLandingOption(base, state))
                .toList();
        boolean holdingRequired = options.stream().noneMatch(LandingOptionDto::canLand);
        GameRound round = gameRoundRepository.findFirstByGame_IdAndEndedAtIsNullOrderByRoundNumberDesc(gameId).orElseThrow();
        return new LandingOptionsDto(gameId, round.getRoundNumber(), aircraftCode, holdingRequired, options);
    }

    @Transactional
    /**
     * Lands an aircraft at the chosen base.
     *
     * <p>Landing consumes a parking slot and may immediately transition the
     * aircraft to maintenance if the base has matching capability, spare parts
     * and a free maintenance slot. Destroyed aircraft are excluded earlier in
     * the flow and cannot be landed.</p>
     */
    public ActionResultDto landAircraft(Long gameId, String aircraftCode, String baseCode) {
        Game game = loadGame(gameId);
        GameRound round = requireActiveRound(gameId, RoundPhase.LANDING);
        GameAircraft aircraft = gameAircraftRepository.findByGame_IdAndCode(gameId, aircraftCode)
                .orElseThrow(() -> new IllegalArgumentException("Aircraft not found: " + aircraftCode));
        GameBase base = gameBaseRepository.findByGame_IdAndCode(gameId, baseCode)
                .orElseThrow(() -> new IllegalArgumentException("Base not found: " + baseCode));
        AircraftState state = aircraftStateRepository.findByGameAircraft_Id(aircraft.getId()).orElseThrow();

        if (aircraft.getStatus() != AircraftStatus.AWAITING_LANDING) {
            return new ActionResultDto(false, "Aircraft is not awaiting landing");
        }

        LandingOptionDto option = toLandingOption(base, state);
        if (!option.canLand()) {
            return new ActionResultDto(false, option.reason());
        }

        BaseState baseState = baseStateRepository.findByGameBase_Id(base.getId()).orElseThrow();
        baseState.incrementOccupiedParkingSlots();
        state.setCurrentBase(base);
        state.setInHolding(false);
        consumeLandingServiceResources(game, round, aircraft, state, base, baseState);
        aircraft.setStatus(resolvePostLandingStatus(game, round, aircraft, state, base, baseState));

        logEvent(game, round, aircraft, base, null, EventType.LANDING,
                aircraftCode + " landed at " + baseCode);
        return new ActionResultDto(true, aircraftCode + " landed at " + baseCode);
    }

    @Transactional
    /**
     * Sends an aircraft to holding if no legal landing base exists.
     *
     * <p>The method rejects the request if at least one base can still accept the
     * aircraft, which prevents the client from bypassing valid landing options.</p>
     */
    public ActionResultDto sendAircraftToHolding(Long gameId, String aircraftCode) {
        Game game = loadGame(gameId);
        GameRound round = requireActiveRound(gameId, RoundPhase.LANDING);
        GameAircraft aircraft = gameAircraftRepository.findByGame_IdAndCode(gameId, aircraftCode)
                .orElseThrow(() -> new IllegalArgumentException("Aircraft not found: " + aircraftCode));
        AircraftState state = aircraftStateRepository.findByGameAircraft_Id(aircraft.getId()).orElseThrow();

        if (aircraft.getStatus() != AircraftStatus.AWAITING_LANDING) {
            return new ActionResultDto(false, "Aircraft is not awaiting landing");
        }
        if (anyBaseCanAccept(gameId, state)) {
            return new ActionResultDto(false, "At least one base can still accept the aircraft");
        }

        aircraft.setStatus(AircraftStatus.HOLDING);
        state.setCurrentBase(null);
        state.setInHolding(true);
        logEvent(game, round, aircraft, null, null, EventType.ENTER_HOLDING,
                aircraftCode + " entered holding");
        return new ActionResultDto(true, aircraftCode + " entered holding");
    }

    @Transactional
    /**
     * Finalizes the active round after all pending decisions are resolved.
     *
     * <p>This method advances maintenance, processes holding effects, applies
     * supply deliveries and recalculates win/loss conditions. After this call
     * the round is closed and a new round may be started if the game remains active.</p>
     */
    public RoundExecutionResultDto completeRound(Long gameId) {
        Game game = loadGame(gameId);
        GameRound round = requireActiveRound(gameId, RoundPhase.LANDING);
        if (hasPendingRoundDecisions(gameId)) {
            throw new IllegalStateException("Round still has pending dice rolls or landings");
        }

        List<String> aircraftUpdates = new ArrayList<>();
        List<String> supplyDeliveries = new ArrayList<>();
        processMaintenanceProgress(game, round, aircraftUpdates);
        tryStartWaitingMaintenance(game, round, aircraftUpdates);
        processHoldingProgress(game, round, aircraftUpdates);
        applySupplyDeliveries(game, round, supplyDeliveries);
        updateGameOutcome(game);

        round.setPhase(RoundPhase.ROUND_COMPLETE);
        round.end(LocalDateTime.now());
        return buildRoundResult(game, round, List.of("Round completed"), List.of(), aircraftUpdates, List.of(), supplyDeliveries);
    }

    /**
     * Advances maintenance timers by one round and releases maintenance slots
     * when repairs complete.
     */
    private void processMaintenanceProgress(Game game, GameRound round, List<String> aircraftUpdates) {
        for (GameAircraft aircraft : gameAircraftRepository.findByGame_Id(game.getId())) {
            AircraftState state = aircraftStateRepository.findByGameAircraft_Id(aircraft.getId()).orElseThrow();
            if (aircraft.getStatus() != AircraftStatus.IN_MAINTENANCE || state.getRepairRoundsRemaining() <= 0) {
                continue;
            }

            state.setRepairRoundsRemaining(state.getRepairRoundsRemaining() - 1);
            aircraftUpdates.add(aircraft.getCode() + " maintenance progress, remaining=" + state.getRepairRoundsRemaining());
            logEvent(game, round, aircraft, state.getCurrentBase(), null, EventType.REPAIR_PROGRESS,
                    aircraft.getCode() + " maintenance progress");

            if (state.getRepairRoundsRemaining() == 0) {
                DamageType completedDamage = state.getDamage();
                state.setDamage(DamageType.NONE);
                BaseState baseState = baseStateRepository.findByGameBase_Id(state.getCurrentBase().getId()).orElseThrow();
                restoreAircraftOperationalState(game, round, aircraft, state, state.getCurrentBase(), baseState,
                        completedDamage == DamageType.FULL_SERVICE_REQUIRED);
                aircraft.setStatus(AircraftStatus.READY);
                baseState.decrementOccupiedMaintSlots();
                logEvent(game, round, aircraft, state.getCurrentBase(), null, EventType.REPAIR_COMPLETE,
                        aircraft.getCode() + " repair completed");
            }
        }
    }

    /**
     * Attempts to move waiting aircraft into maintenance when resources have
     * become available after earlier work completed this round.
     */
    private void tryStartWaitingMaintenance(Game game, GameRound round, List<String> aircraftUpdates) {
        for (GameAircraft aircraft : gameAircraftRepository.findByGame_Id(game.getId())) {
            if (aircraft.getStatus() != AircraftStatus.WAITING_MAINTENANCE) {
                continue;
            }
            AircraftState state = aircraftStateRepository.findByGameAircraft_Id(aircraft.getId()).orElseThrow();
            GameBase base = state.getCurrentBase();
            if (base == null) {
                continue;
            }
            BaseState baseState = baseStateRepository.findByGameBase_Id(base.getId()).orElseThrow();
            if (canStartMaintenance(base, baseState, state)) {
                consumeMaintenanceResources(game, round, aircraft, state, base, baseState);
                aircraft.setStatus(AircraftStatus.IN_MAINTENANCE);
                aircraftUpdates.add(aircraft.getCode() + " entered maintenance");
            }
        }
    }

    /**
     * Applies holding fuel consumption and crash rules.
     *
     * <p>Holding is intentionally resolved only at round completion, which keeps
     * the game flow consistent with the round-based ruleset.</p>
     */
    private void processHoldingProgress(Game game, GameRound round, List<String> aircraftUpdates) {
        for (GameAircraft aircraft : gameAircraftRepository.findByGame_Id(game.getId())) {
            if (aircraft.getStatus() != AircraftStatus.HOLDING) {
                continue;
            }
            AircraftState state = aircraftStateRepository.findByGameAircraft_Id(aircraft.getId()).orElseThrow();
            if (state.getFuel() <= 0) {
                state.setFuel(0);
                state.setInHolding(false);
                aircraft.setStatus(AircraftStatus.CRASHED);
                aircraftUpdates.add(aircraft.getCode() + " crashed after exhausting fuel in holding");
                logEvent(game, round, aircraft, null, null, EventType.CRASH,
                        aircraft.getCode() + " crashed after exhausting fuel in holding");
                continue;
            }
            state.setFuel(Math.max(0, state.getFuel() - HOLDING_FUEL_COST));
            state.setHoldingRounds(state.getHoldingRounds() + 1);
            aircraftUpdates.add(aircraft.getCode() + " remains in holding");
        }
    }

    /**
     * Applies periodic supply rules configured in the scenario for the current
     * round number.
     */
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
                logEvent(game, round, null, gameBase, null, EventType.SUPPLY_DELIVERY, text);
            }
        }
    }

    /**
     * Computes the aircraft state directly after landing.
     *
     * <p>Aircraft with no damage become ready immediately. Damaged aircraft
     * either enter maintenance or wait for maintenance, depending on base capacity
     * and resources. Destroyed aircraft never enter this method in normal flow.</p>
     */
    private AircraftStatus resolvePostLandingStatus(Game game, GameRound round, GameAircraft aircraft,
                                                    AircraftState state, GameBase base, BaseState baseState) {
        if (state.getDamage() == DamageType.DESTROYED) {
            return AircraftStatus.DESTROYED;
        }
        if (state.getDamage() == DamageType.NONE) {
            return AircraftStatus.READY;
        }

        if (canStartMaintenance(base, baseState, state)) {
            consumeMaintenanceResources(game, round, aircraft, state, base, baseState);
            return AircraftStatus.IN_MAINTENANCE;
        }
        return AircraftStatus.WAITING_MAINTENANCE;
    }

    private void consumeMaintenanceResources(Game game, GameRound round, GameAircraft aircraft,
                                             AircraftState state, GameBase base, BaseState baseState) {
        RepairRule repairRule = ruleForDamage(state.getDamage());
        baseState.consumeSpareParts(repairRule.getSparePartsCost());
        baseState.incrementOccupiedMaintSlots();
        resourceTransactionRepository.save(new ResourceTransaction(game, round, base, aircraft, ResourceType.SPARE_PARTS,
                -repairRule.getSparePartsCost(), "REPAIR", LocalDateTime.now()));
        EventType eventType = state.getDamage() == DamageType.FULL_SERVICE_REQUIRED ? EventType.FULL_SERVICE_START : EventType.REPAIR_START;
        logEvent(game, round, aircraft, base, null, eventType,
                aircraft.getCode() + " started " + state.getDamage().name());
    }

    private void consumeLandingServiceResources(Game game, GameRound round, GameAircraft aircraft,
                                                AircraftState state, GameBase base, BaseState baseState) {
        restoreAircraftOperationalState(game, round, aircraft, state, base, baseState, false);
    }

    private void restoreAircraftOperationalState(Game game, GameRound round, GameAircraft aircraft,
                                                 AircraftState state, GameBase base, BaseState baseState,
                                                 boolean restoreFlightHours) {
        int fuelNeeded = Math.max(0, aircraft.getFuelCapacity() - state.getFuel());
        if (fuelNeeded > 0 && baseSupportsService(base, BaseServiceType.REFUEL)) {
            int transferredFuel = Math.min(fuelNeeded, baseState.getFuelStock());
            if (transferredFuel > 0) {
                baseState.consumeFuel(transferredFuel);
                state.setFuel(state.getFuel() + transferredFuel);
                resourceTransactionRepository.save(new ResourceTransaction(game, round, base, aircraft, ResourceType.FUEL,
                        -transferredFuel, "GROUND_SERVICE", LocalDateTime.now()));
            }
        }

        int weaponsNeeded = Math.max(0, aircraft.getWeaponsCapacity() - state.getWeapons());
        if (weaponsNeeded > 0 && baseSupportsService(base, BaseServiceType.REARM)) {
            int transferredWeapons = Math.min(weaponsNeeded, baseState.getWeaponsStock());
            if (transferredWeapons > 0) {
                baseState.consumeWeapons(transferredWeapons);
                state.setWeapons(state.getWeapons() + transferredWeapons);
                resourceTransactionRepository.save(new ResourceTransaction(game, round, base, aircraft, ResourceType.WEAPONS,
                        -transferredWeapons, "GROUND_SERVICE", LocalDateTime.now()));
            }
        }

        if (restoreFlightHours && baseSupportsService(base, BaseServiceType.FULL_SERVICE)) {
            state.setRemainingFlightHours(aircraft.getFlightHoursCapacity());
        }
    }

    private LandingOptionDto toLandingOption(GameBase base, AircraftState state) {
        BaseState baseState = baseStateRepository.findByGameBase_Id(base.getId()).orElseThrow();
        if (baseState.getOccupiedParkingSlots() >= base.getParkingCapacity()) {
            return new LandingOptionDto(base.getCode(), base.getName(), false, "No parking slots available");
        }
        if (state.getDamage() != DamageType.NONE && !baseSupportsDamage(base, state.getDamage())) {
            return new LandingOptionDto(base.getCode(), base.getName(), false, "Base cannot handle " + state.getDamage().name());
        }
        return new LandingOptionDto(base.getCode(), base.getName(), true, "Landing available");
    }

    private boolean baseSupportsDamage(GameBase base, DamageType damageType) {
        if (damageType == DamageType.DESTROYED || damageType == DamageType.NONE) {
            return true;
        }
        if (damageType == DamageType.FULL_SERVICE_REQUIRED) {
            return baseTypeServiceRepository.existsByBaseType_IdAndServiceType(base.getBaseType().getId(), BaseServiceType.FULL_SERVICE);
        }
        return baseTypeServiceRepository.existsByBaseType_IdAndServiceType(base.getBaseType().getId(), BaseServiceType.REPAIR);
    }

    private boolean canStartMaintenance(GameBase base, BaseState baseState, AircraftState state) {
        if (state.getDamage() == DamageType.DESTROYED || state.getDamage() == DamageType.NONE) {
            return false;
        }
        if (!baseSupportsDamage(base, state.getDamage())) {
            return false;
        }
        if (baseState.getOccupiedMaintSlots() >= base.getMaintenanceCapacity()) {
            return false;
        }
        RepairRule rule = ruleForDamage(state.getDamage());
        return baseState.getSparePartsStock() >= rule.getSparePartsCost();
    }

    private RepairRule ruleForDamage(DamageType damageType) {
        return repairRuleRepository.findAll().stream()
                .filter(rule -> rule.getDamage() == damageType)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Repair rule not found for " + damageType));
    }

    private boolean baseSupportsService(GameBase base, BaseServiceType serviceType) {
        return baseTypeServiceRepository.existsByBaseType_IdAndServiceType(base.getBaseType().getId(), serviceType);
    }

    private boolean anyBaseCanAccept(Long gameId, AircraftState state) {
        return gameBaseRepository.findByGame_Id(gameId).stream()
                .map(base -> toLandingOption(base, state))
                .anyMatch(LandingOptionDto::canLand);
    }

    private boolean allAircraftResolvedForDice(Long gameId) {
        return gameAircraftRepository.findByGame_Id(gameId).stream()
                .map(GameAircraft::getStatus)
                .noneMatch(status -> status == AircraftStatus.AWAITING_DICE_ROLL);
    }

    private boolean hasPendingRoundDecisions(Long gameId) {
        return gameAircraftRepository.findByGame_Id(gameId).stream()
                .map(GameAircraft::getStatus)
                .anyMatch(status -> status == AircraftStatus.AWAITING_DICE_ROLL || status == AircraftStatus.AWAITING_LANDING);
    }

    private List<String> pendingAircraft(Long gameId) {
        return gameAircraftRepository.findByGame_Id(gameId).stream()
                .filter(aircraft -> aircraft.getStatus() == AircraftStatus.AWAITING_DICE_ROLL
                        || aircraft.getStatus() == AircraftStatus.AWAITING_LANDING
                        || aircraft.getStatus() == AircraftStatus.HOLDING)
                .map(GameAircraft::getCode)
                .sorted()
                .toList();
    }

    private RoundExecutionResultDto buildRoundResult(Game game, GameRound round, List<String> messages,
                                                     List<String> pendingAircraft, List<String> affectedAircraft,
                                                     List<String> completedMissions, List<String> supplyDeliveries) {
        return new RoundExecutionResultDto(game.getId(), round.getRoundNumber(), round.getPhase().name(),
                game.getStatus().name(), round.isOpen(), pendingAircraft, affectedAircraft,
                completedMissions, supplyDeliveries, messages);
    }

    private Game loadGame(Long gameId) {
        return gameRepository.findById(gameId).orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));
    }

    private void requireGameActive(Game game) {
        if (game.getStatus() != GameStatus.ACTIVE) {
            throw new IllegalStateException("Game is not active");
        }
    }

    private GameRound requireActiveRound(Long gameId, RoundPhase expectedPhase) {
        GameRound round = gameRoundRepository.findFirstByGame_IdAndEndedAtIsNullOrderByRoundNumberDesc(gameId)
                .orElseThrow(() -> new IllegalStateException("No active round"));
        if (round.getPhase() != expectedPhase) {
            throw new IllegalStateException("Round is in phase " + round.getPhase());
        }
        return round;
    }

    /**
     * Evaluates win/loss state after a meaningful game transition.
     */
    private void updateGameOutcome(Game game) {
        List<GameMission> missions = gameMissionRepository.findByGame_IdOrderBySortOrder(game.getId());
        boolean allCompleted = missions.stream().allMatch(m -> m.getStatus() == MissionStatus.COMPLETED);
        List<GameAircraft> aircraft = gameAircraftRepository.findByGame_Id(game.getId());
        boolean allSurvivingAircraftRecovered = aircraft.stream()
                .filter(current -> current.getStatus() != AircraftStatus.CRASHED && current.getStatus() != AircraftStatus.DESTROYED)
                .allMatch(current -> {
                    AircraftState state = aircraftStateRepository.findByGameAircraft_Id(current.getId()).orElseThrow();
                    return current.getStatus() == AircraftStatus.READY && state.getCurrentBase() != null;
                });
        if (allCompleted && allSurvivingAircraftRecovered) {
            game.markWon(LocalDateTime.now());
            return;
        }

        boolean anyOperationalAircraft = aircraft.stream()
                .map(GameAircraft::getStatus)
                .anyMatch(status -> status != AircraftStatus.CRASHED && status != AircraftStatus.DESTROYED);
        if (!anyOperationalAircraft) {
            game.markLost(LocalDateTime.now());
        }
    }

    private void logEvent(Game game, GameRound round, GameAircraft aircraft, GameBase base, GameMission mission,
                          EventType eventType, String details) {
        gameEventRepository.save(new GameEvent(game, round, aircraft, base, mission, eventType, details, LocalDateTime.now()));
    }
}
