package se.smartairbase.mcpclient.service;

import org.springframework.stereotype.Service;
import se.smartairbase.mcpclient.controller.dto.AnalysisFeedItemDTO;
import se.smartairbase.mcpclient.controller.dto.AnalysisFeedResponseDTO;
import se.smartairbase.mcpclient.controller.dto.AircraftStateDTO;
import se.smartairbase.mcpclient.controller.dto.BaseStateDTO;
import se.smartairbase.mcpclient.controller.dto.GameStateDTO;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AnalysisFeedService {

    private final SmartAirBaseMcpClient mcpClient;
    private final Map<String, GameAnalysisState> stateByGameId = new ConcurrentHashMap<>();

    public AnalysisFeedService(SmartAirBaseMcpClient mcpClient) {
        this.mcpClient = mcpClient;
    }

    public AnalysisFeedResponseDTO getFeed(String gameId) {
        GameAnalysisState state = stateByGameId.get(gameId);
        if (state == null) {
            return new AnalysisFeedResponseDTO(List.of(), false, null);
        }
        return new AnalysisFeedResponseDTO(List.copyOf(state.items()), false, state.lastAnalyzedRound());
    }

    public AnalysisFeedResponseDTO generateRoundAnalysis(String gameId) {
        GameStateDTO currentState = mcpClient.getGameStateView(gameId);
        if (currentState.game().currentRound() == null || currentState.game().currentRound() <= 0) {
            return getFeed(gameId);
        }

        stateByGameId.compute(gameId, (ignored, existing) -> {
            GameAnalysisState currentAnalysisState = existing != null ? existing : new GameAnalysisState(new ArrayList<>(), null, null);
            if (Objects.equals(currentAnalysisState.lastAnalyzedRound(), currentState.game().currentRound())) {
                return currentAnalysisState;
            }

            List<AnalysisFeedItemDTO> newItems = buildItems(currentState, currentAnalysisState.lastSnapshot());
            List<AnalysisFeedItemDTO> merged = new ArrayList<>(currentAnalysisState.items());
            merged.addAll(newItems);
            return new GameAnalysisState(merged, currentState.game().currentRound(), snapshot(currentState));
        });

        return getFeed(gameId);
    }

    private List<AnalysisFeedItemDTO> buildItems(GameStateDTO currentState, Snapshot previousSnapshot) {
        Snapshot currentSnapshot = snapshot(currentState);
        String createdAt = OffsetDateTime.now().toString();
        Integer round = currentState.game().currentRound();
        String phase = currentState.game().roundPhase();
        Long gameId = currentState.game().gameId();

        return List.of(
                item(gameId, round, phase, "Pilot",
                        pilotSummary(currentSnapshot, previousSnapshot),
                        pilotDetails(currentSnapshot, previousSnapshot),
                        aircraftForPilot(currentSnapshot, previousSnapshot),
                        List.of(),
                        createdAt),
                item(gameId, round, phase, "Ground Crew",
                        groundCrewSummary(currentSnapshot, previousSnapshot),
                        groundCrewDetails(currentSnapshot, previousSnapshot),
                        aircraftRefueledOrRearmed(currentSnapshot, previousSnapshot),
                        affectedBases(currentSnapshot, previousSnapshot),
                        createdAt),
                item(gameId, round, phase, "Maintenance Technicians",
                        maintenanceSummary(currentSnapshot, previousSnapshot),
                        maintenanceDetails(currentSnapshot, previousSnapshot),
                        aircraftInMaintenance(currentSnapshot),
                        basesWithMaintenanceLoad(currentSnapshot),
                        createdAt),
                item(gameId, round, phase, "Command / Operations",
                        commandSummary(currentSnapshot, previousSnapshot),
                        commandDetails(currentSnapshot),
                        keyAircraft(currentSnapshot),
                        keyBases(currentSnapshot),
                        createdAt)
        );
    }

    private AnalysisFeedItemDTO item(Long gameId, Integer round, String phase, String role,
                                     String summary, String details, List<String> relatedAircraft,
                                     List<String> relatedBases, String createdAt) {
        return new AnalysisFeedItemDTO(
                gameId + "-" + round + "-" + role.replace(' ', '-'),
                gameId,
                round,
                phase,
                role,
                summary,
                details,
                relatedAircraft,
                relatedBases,
                createdAt
        );
    }

    private String pilotSummary(Snapshot current, Snapshot previous) {
        int completedThisRound = current.completedMissionCount() - (previous != null ? previous.completedMissionCount() : 0);
        List<String> returnedAircraft = current.aircraft().values().stream()
                .filter(aircraft -> aircraft.currentBase() != null)
                .map(AircraftStateDTO::code)
                .sorted()
                .toList();
        if (completedThisRound > 0) {
            return completedThisRound + " mission(s) completed this round. Returned aircraft: " + joinOrNone(returnedAircraft) + ".";
        }
        return "No new mission completions this round. Returned aircraft: " + joinOrNone(returnedAircraft) + ".";
    }

    private String pilotDetails(Snapshot current, Snapshot previous) {
        List<String> holdingAircraft = current.aircraftByStatus("HOLDING");
        List<String> destroyedThisRound = statusTransitionedTo(current, previous, Set.of("CRASHED", "DESTROYED"));
        return "Holding: " + joinOrNone(holdingAircraft) + ". Destroyed this round: " + joinOrNone(destroyedThisRound) + ".";
    }

    private String groundCrewSummary(Snapshot current, Snapshot previous) {
        List<String> increasedFuelAircraft = aircraftWithIncreasedMetric(current, previous, AircraftStateDTO::fuel);
        List<String> increasedWeaponsAircraft = aircraftWithIncreasedMetric(current, previous, AircraftStateDTO::weapons);
        return "Refuel observed on " + joinOrNone(increasedFuelAircraft) + ". Rearm observed on " + joinOrNone(increasedWeaponsAircraft) + ".";
    }

    private String groundCrewDetails(Snapshot current, Snapshot previous) {
        List<String> bases = affectedBases(current, previous);
        return "Base resource changes detected at " + joinOrNone(bases) + ".";
    }

    private String maintenanceSummary(Snapshot current, Snapshot previous) {
        List<String> newlyEnteredMaintenance = statusTransitionedTo(current, previous, Set.of("IN_MAINTENANCE", "WAITING_MAINTENANCE"));
        return "Maintenance pressure aircraft: " + joinOrNone(newlyEnteredMaintenance.isEmpty() ? current.aircraftByStatus("IN_MAINTENANCE") : newlyEnteredMaintenance) + ".";
    }

    private String maintenanceDetails(Snapshot current, Snapshot previous) {
        List<String> fullServiceAircraft = current.aircraft().values().stream()
                .filter(aircraft -> "FULL_SERVICE_REQUIRED".equals(aircraft.damage()))
                .map(AircraftStateDTO::code)
                .sorted()
                .toList();
        return "Aircraft requiring full service: " + joinOrNone(fullServiceAircraft) + ".";
    }

    private String commandSummary(Snapshot current, Snapshot previous) {
        int completed = current.completedMissionCount();
        int total = current.missionCount();
        int readyAircraft = current.aircraftByStatus("READY").size();
        int destroyedAircraft = current.aircraftByStatus("CRASHED").size() + current.aircraftByStatus("DESTROYED").size();
        return completed + "/" + total + " missions complete. " + readyAircraft + " aircraft ready. " + destroyedAircraft + " destroyed.";
    }

    private String commandDetails(Snapshot current) {
        String phaseText = current.phase() != null ? current.phase() : "ROUND_COMPLETE";
        return "Round " + current.round() + " ended in " + phaseText + " with game status " + current.gameStatus() + ".";
    }

    private List<String> aircraftForPilot(Snapshot current, Snapshot previous) {
        List<String> involvedAircraft = new ArrayList<>();
        involvedAircraft.addAll(statusTransitionedTo(current, previous, Set.of("READY", "HOLDING", "CRASHED", "DESTROYED")));
        if (involvedAircraft.isEmpty()) {
            involvedAircraft.addAll(current.aircraft().keySet());
        }
        return involvedAircraft.stream().distinct().sorted().toList();
    }

    private List<String> aircraftRefueledOrRearmed(Snapshot current, Snapshot previous) {
        List<String> codes = new ArrayList<>();
        codes.addAll(aircraftWithIncreasedMetric(current, previous, AircraftStateDTO::fuel));
        codes.addAll(aircraftWithIncreasedMetric(current, previous, AircraftStateDTO::weapons));
        return codes.stream().distinct().sorted().toList();
    }

    private List<String> aircraftInMaintenance(Snapshot current) {
        return current.aircraft().values().stream()
                .filter(aircraft -> "IN_MAINTENANCE".equals(aircraft.status()) || "WAITING_MAINTENANCE".equals(aircraft.status()))
                .map(AircraftStateDTO::code)
                .sorted()
                .toList();
    }

    private List<String> affectedBases(Snapshot current, Snapshot previous) {
        if (previous == null) {
            return current.bases().keySet().stream().sorted().toList();
        }
        return current.bases().entrySet().stream()
                .filter(entry -> {
                    BaseStateDTO previousBase = previous.bases().get(entry.getKey());
                    BaseStateDTO currentBase = entry.getValue();
                    return previousBase == null
                            || previousBase.fuelStock() != currentBase.fuelStock()
                            || previousBase.weaponsStock() != currentBase.weaponsStock()
                            || previousBase.sparePartsStock() != currentBase.sparePartsStock()
                            || previousBase.occupiedMaintSlots() != currentBase.occupiedMaintSlots()
                            || previousBase.occupiedParkingSlots() != currentBase.occupiedParkingSlots();
                })
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }

    private List<String> basesWithMaintenanceLoad(Snapshot current) {
        return current.bases().values().stream()
                .filter(base -> base.occupiedMaintSlots() > 0)
                .map(BaseStateDTO::code)
                .sorted()
                .toList();
    }

    private List<String> keyAircraft(Snapshot current) {
        return current.aircraft().values().stream()
                .filter(aircraft -> !"READY".equals(aircraft.status()) || "FULL_SERVICE_REQUIRED".equals(aircraft.damage()))
                .map(AircraftStateDTO::code)
                .sorted()
                .toList();
    }

    private List<String> keyBases(Snapshot current) {
        return current.bases().values().stream()
                .sorted(Comparator.comparingInt(BaseStateDTO::occupiedParkingSlots).reversed())
                .limit(2)
                .map(BaseStateDTO::code)
                .toList();
    }

    private List<String> aircraftWithIncreasedMetric(Snapshot current, Snapshot previous, Function<AircraftStateDTO, Integer> metric) {
        if (previous == null) {
            return List.of();
        }
        return current.aircraft().values().stream()
                .filter(aircraft -> {
                    AircraftStateDTO previousAircraft = previous.aircraft().get(aircraft.code());
                    return previousAircraft != null && metric.apply(aircraft) > metric.apply(previousAircraft);
                })
                .map(AircraftStateDTO::code)
                .sorted()
                .toList();
    }

    private List<String> statusTransitionedTo(Snapshot current, Snapshot previous, Set<String> targetStatuses) {
        if (previous == null) {
            return List.of();
        }
        return current.aircraft().values().stream()
                .filter(aircraft -> targetStatuses.contains(aircraft.status()))
                .filter(aircraft -> {
                    AircraftStateDTO previousAircraft = previous.aircraft().get(aircraft.code());
                    return previousAircraft == null || !Objects.equals(previousAircraft.status(), aircraft.status());
                })
                .map(AircraftStateDTO::code)
                .sorted()
                .toList();
    }

    private Snapshot snapshot(GameStateDTO state) {
        return new Snapshot(
                state.game().currentRound(),
                state.game().roundPhase(),
                state.game().status(),
                state.aircraft().stream().collect(Collectors.toMap(AircraftStateDTO::code, aircraft -> aircraft)),
                state.bases().stream().collect(Collectors.toMap(BaseStateDTO::code, base -> base)),
                (int) state.missions().stream().filter(mission -> "COMPLETED".equals(mission.status())).count(),
                state.missions().size()
        );
    }

    private String joinOrNone(List<String> values) {
        return values.isEmpty() ? "none" : String.join(", ", values);
    }

    private record GameAnalysisState(List<AnalysisFeedItemDTO> items, Integer lastAnalyzedRound, Snapshot lastSnapshot) {
    }

    private record Snapshot(Integer round,
                            String phase,
                            String gameStatus,
                            Map<String, AircraftStateDTO> aircraft,
                            Map<String, BaseStateDTO> bases,
                            int completedMissionCount,
                            int missionCount) {
        private List<String> aircraftByStatus(String status) {
            return aircraft.values().stream()
                    .filter(aircraft -> status.equals(aircraft.status()))
                    .map(AircraftStateDTO::code)
                    .sorted()
                    .toList();
        }
    }
}
