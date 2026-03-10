package se.smartairbase.mcpclient.service;

import org.springframework.stereotype.Service;
import se.smartairbase.mcpclient.controller.dto.AssignMissionRequest;
import se.smartairbase.mcpclient.controller.dto.AutoPlayResponse;
import se.smartairbase.mcpclient.controller.dto.DiceRollRequest;
import se.smartairbase.mcpclient.controller.dto.LandAircraftRequest;
import se.smartairbase.mcpclient.domain.BaseReference;
import se.smartairbase.mcpclient.domain.DiceOutcomeReference;
import se.smartairbase.mcpclient.domain.GameRulesReference;
import se.smartairbase.mcpclient.domain.MissionReference;
import se.smartairbase.mcpclient.service.model.AircraftStateView;
import se.smartairbase.mcpclient.service.model.BaseStateView;
import se.smartairbase.mcpclient.service.model.GameStateView;
import se.smartairbase.mcpclient.service.model.LandingOptionView;
import se.smartairbase.mcpclient.service.model.LandingOptionsView;
import se.smartairbase.mcpclient.service.model.MissionStateView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    public AutoPlayResponse startNextRound(String gameId) {
        List<String> messages = new ArrayList<>();
        List<String> autoAssignments = new ArrayList<>();
        List<String> autoLandings = new ArrayList<>();

        GameStateView stateBefore = mcpClient.getGameStateView(gameId);
        if (!"ACTIVE".equals(stateBefore.game().status())) {
            return buildResponse(stateBefore, false, autoAssignments, autoLandings, messages);
        }

        mcpClient.startRound(gameId);
        messages.add("Round started");

        GameStateView planningState = mcpClient.getGameStateView(gameId);
        for (MissionAssignment assignment : chooseMissionAssignments(planningState)) {
            mcpClient.assignMission(gameId, new AssignMissionRequest(assignment.aircraftCode(), assignment.missionCode()));
            autoAssignments.add(assignment.aircraftCode() + " -> " + assignment.missionCode());
        }

        mcpClient.resolveMissions(gameId);
        messages.add(autoAssignments.isEmpty() ? "No valid mission assignments were available" : "Mission planning resolved");

        GameStateView afterResolution = mcpClient.getGameStateView(gameId);
        boolean roundCompleted = maybeCompleteRound(gameId, afterResolution, messages);
        GameStateView finalState = mcpClient.getGameStateView(gameId);
        return buildResponse(finalState, roundCompleted, autoAssignments, autoLandings, messages);
    }

    /**
     * Records the player's dice roll and lets the client resolve all landing decisions automatically.
     */
    public AutoPlayResponse resolveDiceRoll(String gameId, DiceRollRequest request) {
        List<String> messages = new ArrayList<>();
        List<String> autoAssignments = List.of();
        List<String> autoLandings = new ArrayList<>();

        mcpClient.recordDiceRoll(gameId, request);
        messages.add("Dice roll recorded for " + request.aircraftCode());

        GameStateView state = mcpClient.getGameStateView(gameId);
        if ("LANDING".equals(state.game().roundPhase())) {
            autoResolveLandings(gameId, state, autoLandings, messages);
        }

        GameStateView afterLanding = mcpClient.getGameStateView(gameId);
        boolean roundCompleted = maybeCompleteRound(gameId, afterLanding, messages);
        GameStateView finalState = mcpClient.getGameStateView(gameId);
        return buildResponse(finalState, roundCompleted, autoAssignments, autoLandings, messages);
    }

    private boolean maybeCompleteRound(String gameId, GameStateView state, List<String> messages) {
        if (!state.game().roundOpen() || !state.game().canCompleteRound()) {
            return false;
        }

        mcpClient.completeRound(gameId);
        messages.add("Round completed automatically");
        return true;
    }

    private void autoResolveLandings(String gameId, GameStateView state, List<String> autoLandings, List<String> messages) {
        GameStateView current = state;
        while (true) {
            List<AircraftStateView> pendingLandings = current.aircraft().stream()
                    .filter(aircraft -> "AWAITING_LANDING".equals(aircraft.status()))
                    .sorted(Comparator.comparingInt(this::landingPriority).reversed()
                            .thenComparingInt(AircraftStateView::fuel))
                    .toList();

            if (pendingLandings.isEmpty()) {
                return;
            }

            AircraftStateView aircraft = pendingLandings.getFirst();
            LandingOptionsView landingOptions = mcpClient.getLandingOptionsView(gameId, aircraft.code());
            LandingDecision decision = chooseLandingBase(current, aircraft, landingOptions);

            if (decision.sendToHolding()) {
                mcpClient.sendAircraftToHolding(gameId, aircraft.code());
                autoLandings.add(aircraft.code() + " -> HOLDING");
                messages.add(aircraft.code() + " sent to holding");
            }
            else {
                mcpClient.landAircraft(gameId, new LandAircraftRequest(aircraft.code(), decision.baseCode()));
                autoLandings.add(aircraft.code() + " -> " + decision.baseCode());
                messages.add(aircraft.code() + " landed at " + decision.baseCode());
            }

            current = mcpClient.getGameStateView(gameId);
        }
    }

    private List<MissionAssignment> chooseMissionAssignments(GameStateView state) {
        GameRulesReference rules = rulesReferenceService.getRules();
        Map<String, MissionReference> missionsByCode = rules.missions().stream()
                .collect(Collectors.toMap(MissionReference::code, mission -> mission));

        List<AircraftStateView> readyAircraft = state.aircraft().stream()
                .filter(aircraft -> "READY".equals(aircraft.status()))
                .filter(aircraft -> aircraft.currentBase() != null)
                .filter(aircraft -> !aircraft.inHolding())
                .filter(aircraft -> aircraft.allowedActions() != null && aircraft.allowedActions().contains("ASSIGN_MISSION"))
                .toList();

        List<MissionStateView> availableMissions = state.missions().stream()
                .filter(mission -> "AVAILABLE".equals(mission.status()))
                .sorted(Comparator.comparingInt((MissionStateView mission) -> missionDifficulty(missionsByCode.get(mission.code())))
                        .reversed())
                .toList();

        return searchAssignments(availableMissions, 0, readyAircraft, new HashSet<>(), new ArrayList<>(), missionsByCode)
                .assignments();
    }

    private AssignmentSearchResult searchAssignments(List<MissionStateView> missions,
                                                     int index,
                                                     List<AircraftStateView> aircraft,
                                                     Set<String> usedAircraft,
                                                     List<MissionAssignment> currentAssignments,
                                                     Map<String, MissionReference> missionsByCode) {
        if (index >= missions.size()) {
            return scoreAssignments(currentAssignments, aircraft, missionsByCode);
        }

        MissionStateView mission = missions.get(index);
        AssignmentSearchResult best = searchAssignments(missions, index + 1, aircraft, usedAircraft, currentAssignments, missionsByCode);

        for (AircraftStateView candidate : aircraft) {
            if (usedAircraft.contains(candidate.code())) {
                continue;
            }
            MissionReference missionReference = missionsByCode.get(mission.code());
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
                                                    List<AircraftStateView> aircraft,
                                                    Map<String, MissionReference> missionsByCode) {
        Map<String, AircraftStateView> aircraftByCode = aircraft.stream()
                .collect(Collectors.toMap(AircraftStateView::code, aircraftState -> aircraftState));

        int totalMissionValue = 0;
        int totalWaste = 0;
        for (MissionAssignment assignment : assignments) {
            MissionReference mission = missionsByCode.get(assignment.missionCode());
            AircraftStateView aircraftState = aircraftByCode.get(assignment.aircraftCode());
            totalMissionValue += missionDifficulty(mission);
            totalWaste += (aircraftState.fuel() - mission.fuelCost())
                    + ((aircraftState.weapons() - mission.weaponCost()) * 20)
                    + ((aircraftState.remainingFlightHours() - mission.flightHours()) * 5);
        }

        return new AssignmentSearchResult(List.copyOf(assignments), assignments.size(), totalMissionValue, totalWaste);
    }

    private boolean canAircraftFlyMission(AircraftStateView aircraft, MissionReference mission) {
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

    private LandingDecision chooseLandingBase(GameStateView state, AircraftStateView aircraft, LandingOptionsView landingOptions) {
        List<LandingOptionView> validOptions = landingOptions.options().stream()
                .filter(LandingOptionView::canLand)
                .toList();
        if (validOptions.isEmpty()) {
            return LandingDecision.holding();
        }

        GameRulesReference rules = rulesReferenceService.getRules();
        Map<String, BaseReference> baseReferenceByCode = rules.bases().stream()
                .collect(Collectors.toMap(BaseReference::code, base -> base));
        Map<String, BaseStateView> baseStateByCode = state.bases().stream()
                .collect(Collectors.toMap(BaseStateView::code, base -> base));
        List<MissionReference> remainingMissions = remainingMissionReferences(state, rules);

        LandingOptionView best = validOptions.stream()
                .max(Comparator.comparingInt(option -> landingScore(
                        aircraft,
                        option,
                        baseReferenceByCode.get(option.baseCode()),
                        baseStateByCode.get(option.baseCode()),
                        remainingMissions,
                        validOptions)))
                .orElseThrow();

        return LandingDecision.land(best.baseCode());
    }

    private List<MissionReference> remainingMissionReferences(GameStateView state, GameRulesReference rules) {
        Map<String, MissionReference> missionByCode = rules.missions().stream()
                .collect(Collectors.toMap(MissionReference::code, mission -> mission));
        return state.missions().stream()
                .filter(mission -> !"COMPLETED".equals(mission.status()))
                .map(mission -> missionByCode.get(mission.code()))
                .filter(Objects::nonNull)
                .toList();
    }

    private int landingScore(AircraftStateView aircraft,
                             LandingOptionView option,
                             BaseReference baseReference,
                             BaseStateView baseState,
                             List<MissionReference> remainingMissions,
                             List<LandingOptionView> validOptions) {
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
                    && !"A".equals(option.baseCode())
                    && validOptions.stream().anyMatch(candidate -> "A".equals(candidate.baseCode()))) {
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
                if ("A".equals(option.baseCode())
                        && validOptions.stream().anyMatch(candidate -> {
                            BaseReference candidateBase = baseReferenceFor(validOptions, candidate.baseCode(), baseReference, remainingMissions);
                            return candidateBase != null && !"A".equals(candidateBase.code()) && canRearm(candidateBase);
                        })) {
                    score -= 20;
                }
            }
            else if ("C".equals(option.baseCode())) {
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

    private BaseReference baseReferenceFor(List<LandingOptionView> validOptions,
                                           String baseCode,
                                           BaseReference ignored,
                                           List<MissionReference> remainingMissions) {
        return rulesReferenceService.getRules().bases().stream()
                .filter(base -> base.code().equals(baseCode))
                .findFirst()
                .orElse(null);
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

    private boolean canStartMaintenance(BaseReference base, BaseStateView state, String damage) {
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
        return base.capabilities().stream()
                .map(String::toLowerCase)
                .toList();
    }

    private int sparePartsCostFor(String damage) {
        DiceOutcomeReference rule = diceRulesByDamage().get(damage);
        return rule != null ? rule.sparePartsCost() : 0;
    }

    private int landingPriority(AircraftStateView aircraft) {
        return switch (aircraft.damage()) {
            case "FULL_SERVICE_REQUIRED" -> 5;
            case "MAJOR_REPAIR" -> 4;
            case "COMPONENT_DAMAGE" -> 3;
            case "MINOR_REPAIR" -> 2;
            default -> 1;
        };
    }

    private Map<String, DiceOutcomeReference> diceRulesByDamage() {
        Map<String, DiceOutcomeReference> byDamage = new HashMap<>();
        byDamage.put("NONE", new DiceOutcomeReference(1, "No fault", 0, 0));
        byDamage.put("MINOR_REPAIR", new DiceOutcomeReference(2, "Minor repair", 1, 1));
        byDamage.put("COMPONENT_DAMAGE", new DiceOutcomeReference(4, "Component damage", 2, 2));
        byDamage.put("MAJOR_REPAIR", new DiceOutcomeReference(5, "Major repair", 3, 3));
        byDamage.put("FULL_SERVICE_REQUIRED", new DiceOutcomeReference(6, "Full service required", 4, 4));
        return byDamage;
    }

    private AutoPlayResponse buildResponse(GameStateView state,
                                           boolean roundCompleted,
                                           List<String> autoAssignments,
                                           List<String> autoLandings,
                                           List<String> messages) {
        List<String> pendingDiceAircraft = state.aircraft().stream()
                .filter(aircraft -> "AWAITING_DICE_ROLL".equals(aircraft.status()))
                .map(AircraftStateView::code)
                .sorted()
                .toList();
        boolean gameFinished = !"ACTIVE".equals(state.game().status());
        return new AutoPlayResponse(
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

    private String nextAction(GameStateView state, List<String> pendingDiceAircraft) {
        if (!"ACTIVE".equals(state.game().status())) {
            return "GAME_OVER";
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
