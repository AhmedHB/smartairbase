package se.smartairbase.mcpclient.service;

import org.springframework.stereotype.Service;
import se.smartairbase.mcpclient.controller.dto.AircraftStateDTO;
import se.smartairbase.mcpclient.controller.dto.AssignMissionRequestDTO;
import se.smartairbase.mcpclient.controller.dto.BaseStateDTO;
import se.smartairbase.mcpclient.controller.dto.AutoPlayResponseDTO;
import se.smartairbase.mcpclient.controller.dto.DiceRollRequestDTO;
import se.smartairbase.mcpclient.controller.dto.GameStateDTO;
import se.smartairbase.mcpclient.controller.dto.LandAircraftRequestDTO;
import se.smartairbase.mcpclient.controller.dto.LandingOptionDTO;
import se.smartairbase.mcpclient.controller.dto.LandingOptionsDTO;
import se.smartairbase.mcpclient.controller.dto.MissionStateDTO;
import se.smartairbase.mcpclient.domain.BaseReference;
import se.smartairbase.mcpclient.domain.DiceOutcomeReference;
import se.smartairbase.mcpclient.domain.MissionReference;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Client-side autopilot for mission planning and landing decisions.
 *
 * <p>The MCP server remains authoritative for all rule validation. This service
 * only decides which valid action to attempt next based on current game state
 * and the local scenario reference.</p>
 */
@Service
public class AutoPlayService {

    private final SmartAirBaseMcpClient mcpClient;
    private final GameRulesReferenceService rulesReferenceService;

    public AutoPlayService(SmartAirBaseMcpClient mcpClient, GameRulesReferenceService rulesReferenceService) {
        this.mcpClient = mcpClient;
        this.rulesReferenceService = rulesReferenceService;
    }

    /**
     * Starts the next round and lets the client perform planning automatically.
     */
    public AutoPlayResponseDTO startNextRound(String gameId) {
        List<String> messages = new ArrayList<>();
        List<String> autoAssignments = new ArrayList<>();
        List<String> autoLandings = new ArrayList<>();

        GameStateDTO stateBefore = mcpClient.getGameStateView(gameId);
        if (!"ACTIVE".equals(stateBefore.game().status())) {
            return buildResponse(stateBefore, false, autoAssignments, autoLandings, messages);
        }

        mcpClient.startRound(gameId);
        messages.add("Round started");

        GameStateDTO planningState = mcpClient.getGameStateView(gameId);
        for (MissionAssignment assignment : chooseMissionAssignments(planningState)) {
            mcpClient.assignMission(gameId, new AssignMissionRequestDTO(assignment.aircraftCode(), assignment.missionCode()));
            autoAssignments.add(assignment.aircraftCode() + " -> " + assignment.missionCode());
        }

        mcpClient.resolveMissions(gameId);
        messages.add(autoAssignments.isEmpty() ? "No valid mission assignments were available" : "Mission planning resolved");

        GameStateDTO afterResolution = mcpClient.getGameStateView(gameId);
        boolean roundCompleted = maybeCompleteRound(gameId, afterResolution, messages);
        GameStateDTO finalState = mcpClient.getGameStateView(gameId);
        return buildResponse(finalState, roundCompleted, autoAssignments, autoLandings, messages);
    }

    /**
     * Starts a new round and performs automatic mission assignment without resolving the missions yet.
     */
    public AutoPlayResponseDTO planNextRound(String gameId) {
        List<String> messages = new ArrayList<>();
        List<String> autoAssignments = new ArrayList<>();
        List<String> autoLandings = List.of();

        GameStateDTO stateBefore = mcpClient.getGameStateView(gameId);
        if (!"ACTIVE".equals(stateBefore.game().status())) {
            return buildResponse(stateBefore, false, autoAssignments, autoLandings, messages);
        }

        mcpClient.startRound(gameId);
        messages.add("Round started");

        GameStateDTO planningState = mcpClient.getGameStateView(gameId);
        for (MissionAssignment assignment : chooseMissionAssignments(planningState)) {
            mcpClient.assignMission(gameId, new AssignMissionRequestDTO(assignment.aircraftCode(), assignment.missionCode()));
            autoAssignments.add(assignment.aircraftCode() + " -> " + assignment.missionCode());
        }

        GameStateDTO finalState = mcpClient.getGameStateView(gameId);
        messages.add(autoAssignments.isEmpty() ? "No valid mission assignments were available" : "Mission planning prepared");
        return buildResponse(finalState, false, autoAssignments, autoLandings, messages);
    }

    /**
     * Resolves already assigned missions and moves the round forward to dice, landing or completion.
     */
    public AutoPlayResponseDTO resolvePlannedMissions(String gameId) {
        List<String> messages = new ArrayList<>();
        List<String> autoAssignments = List.of();
        List<String> autoLandings = new ArrayList<>();

        GameStateDTO stateBefore = mcpClient.getGameStateView(gameId);
        if (!stateBefore.game().roundOpen() || !"PLANNING".equals(stateBefore.game().roundPhase())) {
            messages.add("Mission resolution is not available");
            return buildResponse(stateBefore, false, autoAssignments, autoLandings, messages);
        }

        mcpClient.resolveMissions(gameId);
        messages.add("Mission resolution completed");

        GameStateDTO afterResolution = mcpClient.getGameStateView(gameId);
        boolean roundCompleted = maybeCompleteRound(gameId, afterResolution, messages);
        GameStateDTO finalState = mcpClient.getGameStateView(gameId);
        return buildResponse(finalState, roundCompleted, autoAssignments, autoLandings, messages);
    }

    /**
     * Records the player's dice roll and lets the client resolve any follow-up
     * landing decisions automatically.
     *
     * <p>If the dice outcome destroys the aircraft, no landing is attempted and
     * the client can proceed directly toward round completion.</p>
     */
    public AutoPlayResponseDTO resolveDiceRoll(String gameId, DiceRollRequestDTO request) {
        List<String> messages = new ArrayList<>();
        List<String> autoAssignments = List.of();
        List<String> autoLandings = new ArrayList<>();

        GameStateDTO stateBefore = mcpClient.getGameStateView(gameId);
        boolean aircraftPending = stateBefore.aircraft().stream()
                .anyMatch(aircraft -> request.aircraftCode().equals(aircraft.code())
                        && "AWAITING_DICE_ROLL".equals(aircraft.status()));
        if (!"DICE_ROLL".equals(stateBefore.game().roundPhase()) || !aircraftPending) {
            messages.add("Dice step already finished");
            return buildResponse(stateBefore, false, autoAssignments, autoLandings, messages);
        }

        try {
            mcpClient.recordDiceRoll(gameId, request);
            messages.add("Dice roll recorded for " + request.aircraftCode());
        }
        catch (IllegalStateException exception) {
            if (isLateDiceRoll(exception)) {
                GameStateDTO currentState = mcpClient.getGameStateView(gameId);
                messages.add("Dice step already finished");
                return buildResponse(currentState, false, autoAssignments, autoLandings, messages);
            }
            throw exception;
        }

        GameStateDTO state = mcpClient.getGameStateView(gameId);
        if (state.aircraft().stream().anyMatch(aircraft -> request.aircraftCode().equals(aircraft.code())
                && "DESTROYED".equals(aircraft.status()))) {
            messages.add(request.aircraftCode() + " destroyed");
        }
        if ("LANDING".equals(state.game().roundPhase())) {
            autoResolveLandings(gameId, state, autoLandings, messages);
        }

        GameStateDTO afterLanding = mcpClient.getGameStateView(gameId);
        boolean roundCompleted = maybeCompleteRound(gameId, afterLanding, messages);
        GameStateDTO finalState = mcpClient.getGameStateView(gameId);
        return buildResponse(finalState, roundCompleted, autoAssignments, autoLandings, messages);
    }

    private boolean isLateDiceRoll(IllegalStateException exception) {
        return exception.getMessage() != null
                && exception.getMessage().contains("Round is in phase LANDING");
    }

    private boolean maybeCompleteRound(String gameId, GameStateDTO state, List<String> messages) {
        if (!state.game().roundOpen()) {
            return false;
        }
        boolean noPendingRoundDecisions = state.aircraft().stream()
                .noneMatch(aircraft -> "AWAITING_DICE_ROLL".equals(aircraft.status())
                        || "AWAITING_LANDING".equals(aircraft.status()));
        boolean canSafelyComplete = state.game().canCompleteRound()
                || ("LANDING".equals(state.game().roundPhase()) && noPendingRoundDecisions);
        if (!canSafelyComplete) {
            return false;
        }

        mcpClient.completeRound(gameId);
        messages.add("Round completed automatically");
        return true;
    }

    private void autoResolveLandings(String gameId, GameStateDTO state, List<String> autoLandings, List<String> messages) {
        GameStateDTO current = state;
        while (true) {
            List<AircraftStateDTO> pendingLandings = current.aircraft().stream()
                    .filter(aircraft -> "AWAITING_LANDING".equals(aircraft.status()))
                    .sorted(Comparator.comparingInt(this::landingPriority).reversed()
                            .thenComparingInt(AircraftStateDTO::fuel))
                    .toList();

            if (pendingLandings.isEmpty()) {
                return;
            }

            AircraftStateDTO aircraft = pendingLandings.getFirst();
            LandingOptionsDTO landingOptions = mcpClient.getLandingOptionsView(gameId, aircraft.code());
            LandingDecision decision = chooseLandingBase(current, aircraft, landingOptions);

            if (decision.sendToHolding()) {
                mcpClient.sendAircraftToHolding(gameId, aircraft.code());
                autoLandings.add(aircraft.code() + " -> HOLDING");
                messages.add(aircraft.code() + " sent to holding");
            }
            else {
                mcpClient.landAircraft(gameId, new LandAircraftRequestDTO(aircraft.code(), decision.baseCode()));
                autoLandings.add(aircraft.code() + " -> " + decision.baseCode());
                messages.add(aircraft.code() + " landed at " + decision.baseCode());
            }

            current = mcpClient.getGameStateView(gameId);
        }
    }

    private List<MissionAssignment> chooseMissionAssignments(GameStateDTO state) {
        Map<String, MissionReference> missionsByCode = missionReferenceByCode();

        List<AircraftStateDTO> readyAircraft = state.aircraft().stream()
                .filter(aircraft -> "READY".equals(aircraft.status()))
                .filter(aircraft -> aircraft.currentBase() != null)
                .filter(aircraft -> !aircraft.inHolding())
                .filter(aircraft -> aircraft.allowedActions() != null && aircraft.allowedActions().contains("ASSIGN_MISSION"))
                .toList();

        List<MissionStateDTO> availableMissions = state.missions().stream()
                .filter(mission -> "AVAILABLE".equals(mission.status()))
                .sorted(Comparator.comparingInt((MissionStateDTO mission) -> missionDifficulty(missionsByCode.get(mission.missionType())))
                        .reversed())
                .toList();

        return searchAssignments(availableMissions, 0, readyAircraft, new HashSet<>(), new ArrayList<>(), missionsByCode)
                .assignments();
    }

    private AssignmentSearchResult searchAssignments(List<MissionStateDTO> missions,
                                                     int index,
                                                     List<AircraftStateDTO> aircraft,
                                                     Set<String> usedAircraft,
                                                     List<MissionAssignment> currentAssignments,
                                                     Map<String, MissionReference> missionsByCode) {
        if (index >= missions.size()) {
            return scoreAssignments(currentAssignments, aircraft, missionsByCode);
        }

        MissionStateDTO mission = missions.get(index);
        AssignmentSearchResult best = searchAssignments(missions, index + 1, aircraft, usedAircraft, currentAssignments, missionsByCode);

        for (AircraftStateDTO candidate : aircraft) {
            if (usedAircraft.contains(candidate.code())) {
                continue;
            }
            MissionReference missionReference = missionsByCode.get(mission.missionType());
            if (!canAircraftFlyMission(candidate, missionReference)) {
                continue;
            }

            usedAircraft.add(candidate.code());
            currentAssignments.add(new MissionAssignment(candidate.code(), mission.code()));
            AssignmentSearchResult result = searchAssignments(missions, index + 1, aircraft, usedAircraft, currentAssignments, missionsByCode);
            if (result.isBetterThan(best)) {
                best = result;
            }
            currentAssignments.removeLast();
            usedAircraft.remove(candidate.code());
        }

        return best;
    }

    private AssignmentSearchResult scoreAssignments(List<MissionAssignment> assignments,
                                                    List<AircraftStateDTO> aircraft,
                                                    Map<String, MissionReference> missionsByCode) {
        Map<String, AircraftStateDTO> aircraftByCode = aircraft.stream()
                .collect(Collectors.toMap(AircraftStateDTO::code, aircraftState -> aircraftState));

        int totalMissionValue = 0;
        int totalWaste = 0;
        for (MissionAssignment assignment : assignments) {
            MissionReference mission = missionsByCode.get(missionTypeCode(assignment.missionCode()));
            AircraftStateDTO aircraftState = aircraftByCode.get(assignment.aircraftCode());
            totalMissionValue += missionDifficulty(mission);
            totalWaste += (aircraftState.fuel() - mission.fuelCost())
                    + ((aircraftState.weapons() - mission.weaponCost()) * 20)
                    + ((aircraftState.remainingFlightHours() - mission.flightHours()) * 5);
        }

        return new AssignmentSearchResult(List.copyOf(assignments), assignments.size(), totalMissionValue, totalWaste);
    }

    private boolean canAircraftFlyMission(AircraftStateDTO aircraft, MissionReference mission) {
        if (mission == null) {
            return false;
        }
        if (aircraft.fuel() < mission.fuelCost()) {
            return false;
        }
        if (aircraft.weapons() < mission.weaponCost()) {
            return false;
        }
        if (aircraft.remainingFlightHours() < mission.flightHours()) {
            return false;
        }
        return !"FULL_SERVICE_REQUIRED".equals(aircraft.damage());
    }

    private int missionDifficulty(MissionReference mission) {
        return mission.flightHours() * 100 + mission.fuelCost() * 10 + mission.weaponCost() * 40;
    }

    private LandingDecision chooseLandingBase(GameStateDTO state, AircraftStateDTO aircraft, LandingOptionsDTO landingOptions) {
        List<LandingOptionDTO> validOptions = landingOptions.options().stream()
                .filter(LandingOptionDTO::canLand)
                .toList();
        if (validOptions.isEmpty()) {
            return LandingDecision.holding();
        }

        Map<String, BaseReference> baseReferenceByCode = baseReferenceByCode();
        Map<String, BaseStateDTO> baseStateByCode = state.bases().stream()
                .collect(Collectors.toMap(base -> normalizeBaseCode(base.code()), base -> base, (first, second) -> first));
        List<MissionReference> remainingMissions = remainingMissionReferences(state);
        boolean alternativeRearmBaseExists = validOptions.stream()
                .map(candidate -> baseReferenceByCode.get(normalizeBaseCode(candidate.baseCode())))
                .anyMatch(candidateBase -> candidateBase != null
                        && !"A".equals(candidateBase.code())
                        && canRearm(candidateBase));

        LandingOptionDTO best = validOptions.stream()
                .max(Comparator.comparingInt(option -> landingScore(
                        aircraft,
                        option,
                        baseReferenceByCode.get(normalizeBaseCode(option.baseCode())),
                        baseStateByCode.get(normalizeBaseCode(option.baseCode())),
                        remainingMissions,
                        validOptions,
                        alternativeRearmBaseExists)))
                .orElseThrow();

        return LandingDecision.land(best.baseCode());
    }

    private int landingScore(AircraftStateDTO aircraft,
                             LandingOptionDTO option,
                             BaseReference baseReference,
                             BaseStateDTO baseState,
                             List<MissionReference> remainingMissions,
                             List<LandingOptionDTO> validOptions,
                             boolean alternativeRearmBaseExists) {
        int score = 0;
        String damage = aircraft.damage();
        int maxRemainingWeaponCost = remainingMissions.stream().mapToInt(MissionReference::weaponCost).max().orElse(0);
        int maxRemainingFuelCost = remainingMissions.stream().mapToInt(MissionReference::fuelCost).max().orElse(0);

        if (!"NONE".equals(damage)) {
            if (canStartMaintenance(baseReference, baseState, damage)) {
                score += 500;
            }
            else if (supportsDamage(baseReference, damage)) {
                score += 250;
            }

            if (!"FULL_SERVICE_REQUIRED".equals(damage)
                    && !"A".equals(normalizeBaseCode(option.baseCode()))
                    && validOptions.stream().anyMatch(candidate -> "A".equals(normalizeBaseCode(candidate.baseCode())))) {
                score += 40;
            }
        }
        else {
            if (maxRemainingWeaponCost > 0) {
                if (canRearm(baseReference)) {
                    score += 150;
                }
                else {
                    score -= 300;
                }
                if (baseState != null && baseState.weaponsStock() >= maxRemainingWeaponCost) {
                    score += 60;
                }
                if ("A".equals(normalizeBaseCode(option.baseCode())) && alternativeRearmBaseExists) {
                    score -= 20;
                }
            }
            else if ("C".equals(normalizeBaseCode(option.baseCode()))) {
                score += 25;
            }
        }

        if (baseState != null) {
            score += Math.max(0, baseState.parkingCapacity() - baseState.occupiedParkingSlots()) * 5;
            if (maxRemainingFuelCost > 0) {
                score += Math.min(baseState.fuelStock(), maxRemainingFuelCost * 2);
            }
            if (baseState.sparePartsStock() > 0 && !"NONE".equals(damage)) {
                score += Math.min(baseState.sparePartsStock(), sparePartsCostFor(damage)) * 20;
            }
        }

        return score;
    }

    private boolean supportsDamage(BaseReference base, String damage) {
        if (base == null || damage == null || "NONE".equals(damage)) {
            return true;
        }
        List<String> capabilities = normalizeCapabilities(base);
        if ("FULL_SERVICE_REQUIRED".equals(damage)) {
            return capabilities.contains("full service");
        }
        return capabilities.contains("repair") || capabilities.contains("light repair");
    }

    private boolean canStartMaintenance(BaseReference base, BaseStateDTO state, String damage) {
        if (base == null || state == null || "NONE".equals(damage)) {
            return false;
        }
        if (!supportsDamage(base, damage)) {
            return false;
        }
        if (state.occupiedMaintSlots() >= state.maintenanceCapacity()) {
            return false;
        }
        return state.sparePartsStock() >= sparePartsCostFor(damage);
    }

    private boolean canRearm(BaseReference base) {
        return normalizeCapabilities(base).contains("rearm");
    }

    private List<String> normalizeCapabilities(BaseReference base) {
        if (base == null || base.capabilities() == null) {
            return List.of();
        }
        return base.capabilities().stream()
                .map(String::toLowerCase)
                .toList();
    }

    private int sparePartsCostFor(String damage) {
        DiceOutcomeReference rule = diceRulesByDamage().get(damage);
        return rule != null ? rule.sparePartsCost() : 0;
    }

    private List<MissionReference> remainingMissionReferences(GameStateDTO state) {
        Map<String, MissionReference> missionByCode = missionReferenceByCode();
        return state.missions().stream()
                .filter(mission -> !"COMPLETED".equals(mission.status()))
                .map(mission -> missionByCode.get(mission.missionType()))
                .filter(Objects::nonNull)
                .toList();
    }

    private Map<String, MissionReference> missionReferenceByCode() {
        return rulesReferenceService.getRules().missions().stream()
                .collect(Collectors.toMap(MissionReference::code, mission -> mission));
    }

    private String missionTypeCode(String missionCode) {
        int separator = missionCode.indexOf('-');
        return separator > 0 ? missionCode.substring(0, separator) : missionCode;
    }

    private Map<String, BaseReference> baseReferenceByCode() {
        return rulesReferenceService.getRules().bases().stream()
                .collect(Collectors.toMap(base -> normalizeBaseCode(base.code()), base -> base, (first, second) -> first));
    }

    private String normalizeBaseCode(String code) {
        if (code == null) {
            return "";
        }
        return code.toUpperCase(Locale.ROOT).replace("BASE_", "");
    }

    private int landingPriority(AircraftStateDTO aircraft) {
        return switch (aircraft.damage()) {
            case "DESTROYED" -> 6;
            case "FULL_SERVICE_REQUIRED" -> 5;
            case "MAJOR_REPAIR" -> 4;
            case "COMPONENT_DAMAGE" -> 3;
            case "MINOR_REPAIR" -> 2;
            default -> 1;
        };
    }

    private Map<String, DiceOutcomeReference> diceRulesByDamage() {
        Map<String, DiceOutcomeReference> byDamage = new HashMap<>();
        byDamage.put("DESTROYED", new DiceOutcomeReference(1, "Destroyed", 0, 0));
        byDamage.put("FULL_SERVICE_REQUIRED", new DiceOutcomeReference(2, "Full service required", 4, 4));
        byDamage.put("MAJOR_REPAIR", new DiceOutcomeReference(3, "Major repair", 3, 3));
        byDamage.put("COMPONENT_DAMAGE", new DiceOutcomeReference(4, "Component damage", 2, 2));
        byDamage.put("MINOR_REPAIR", new DiceOutcomeReference(5, "Minor repair", 1, 1));
        byDamage.put("NONE", new DiceOutcomeReference(6, "No fault", 0, 0));
        return byDamage;
    }

    private AutoPlayResponseDTO buildResponse(GameStateDTO state,
                                              boolean roundCompleted,
                                              List<String> autoAssignments,
                                              List<String> autoLandings,
                                              List<String> messages) {
        List<String> pendingDiceAircraft = state.aircraft().stream()
                .filter(aircraft -> "AWAITING_DICE_ROLL".equals(aircraft.status()))
                .map(AircraftStateDTO::code)
                .sorted()
                .toList();
        boolean gameFinished = !"ACTIVE".equals(state.game().status());
        return new AutoPlayResponseDTO(
                state,
                nextAction(state, pendingDiceAircraft),
                roundCompleted,
                gameFinished,
                pendingDiceAircraft,
                List.copyOf(autoAssignments),
                List.copyOf(autoLandings),
                List.copyOf(messages)
        );
    }

    private String nextAction(GameStateDTO state, List<String> pendingDiceAircraft) {
        if (!"ACTIVE".equals(state.game().status())) {
            return "GAME_OVER";
        }
        if (state.game().roundOpen() && "PLANNING".equals(state.game().roundPhase())) {
            return "RESOLVE_MISSIONS";
        }
        if (!pendingDiceAircraft.isEmpty()) {
            return "ROLL_DICE";
        }
        if (state.game().canStartRound() && !state.game().roundOpen()) {
            return "START_NEXT_ROUND";
        }
        return "WAIT";
    }

    private record MissionAssignment(String aircraftCode, String missionCode) {
    }

    private record AssignmentSearchResult(List<MissionAssignment> assignments, int assignmentCount, int missionValue, int totalWaste) {

        private boolean isBetterThan(AssignmentSearchResult other) {
            if (other == null) {
                return true;
            }
            if (assignmentCount != other.assignmentCount) {
                return assignmentCount > other.assignmentCount;
            }
            if (missionValue != other.missionValue) {
                return missionValue > other.missionValue;
            }
            return totalWaste < other.totalWaste;
        }
    }

    private record LandingDecision(String baseCode, boolean sendToHolding) {

        private static LandingDecision land(String baseCode) {
            return new LandingDecision(baseCode, false);
        }

        private static LandingDecision holding() {
            return new LandingDecision(null, true);
        }
    }
}
