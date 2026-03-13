package se.smartairbase.mcpclient.service.analysis;

import org.springframework.stereotype.Service;
import se.smartairbase.mcpclient.controller.dto.AircraftStateDTO;
import se.smartairbase.mcpclient.controller.dto.BaseStateDTO;
import se.smartairbase.mcpclient.controller.dto.GameStateDTO;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
/**
 * Builds structured round facts from successive game-state snapshots.
 */
public class AnalysisFactService {

    public AnalysisRoundFacts buildFacts(GameStateDTO currentState, Snapshot previousSnapshot) {
        Snapshot currentSnapshot = snapshot(currentState);
        return new AnalysisRoundFacts(
                currentState.game().gameId(),
                currentState.game().currentRound(),
                currentState.game().roundPhase(),
                currentState.game().status(),
                currentSnapshot.completedMissionCount(),
                currentSnapshot.missionCount(),
                currentSnapshot.aircraftByStatus("READY").size(),
                currentSnapshot.aircraftByStatus("CRASHED").size() + currentSnapshot.aircraftByStatus("DESTROYED").size(),
                landedAircraft(currentSnapshot),
                currentSnapshot.aircraftByStatus("HOLDING"),
                statusTransitionedTo(currentSnapshot, previousSnapshot, Set.of("CRASHED", "DESTROYED")),
                currentSnapshot.aircraft().values().stream()
                        .filter(aircraft -> "IN_MAINTENANCE".equals(aircraft.status()) || "WAITING_MAINTENANCE".equals(aircraft.status()))
                        .map(AircraftStateDTO::code)
                        .sorted()
                        .toList(),
                currentSnapshot.aircraft().values().stream()
                        .filter(aircraft -> "FULL_SERVICE_REQUIRED".equals(aircraft.damage()))
                        .map(AircraftStateDTO::code)
                        .sorted()
                        .toList(),
                aircraftWithIncreasedMetric(currentSnapshot, previousSnapshot, AircraftStateDTO::fuel),
                aircraftWithIncreasedMetric(currentSnapshot, previousSnapshot, AircraftStateDTO::weapons),
                affectedBases(currentSnapshot, previousSnapshot),
                currentSnapshot.aircraft().values().stream()
                        .filter(aircraft -> !"READY".equals(aircraft.status()) || "FULL_SERVICE_REQUIRED".equals(aircraft.damage()))
                        .map(AircraftStateDTO::code)
                        .sorted()
                        .toList(),
                currentSnapshot.bases().values().stream()
                        .sorted((left, right) -> Integer.compare(right.occupiedParkingSlots(), left.occupiedParkingSlots()))
                        .limit(2)
                        .map(BaseStateDTO::code)
                        .toList()
        );
    }

    public Snapshot snapshot(GameStateDTO state) {
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

    private List<String> landedAircraft(Snapshot current) {
        return current.aircraft().values().stream()
                .filter(aircraft -> aircraft.currentBase() != null)
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

    public record Snapshot(Integer round,
                           String phase,
                           String gameStatus,
                           Map<String, AircraftStateDTO> aircraft,
                           Map<String, BaseStateDTO> bases,
                           int completedMissionCount,
                           int missionCount) {
        public List<String> aircraftByStatus(String status) {
            return aircraft.values().stream()
                    .filter(aircraft -> status.equals(aircraft.status()))
                    .map(AircraftStateDTO::code)
                    .sorted()
                    .toList();
        }
    }
}
