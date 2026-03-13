package se.smartairbase.mcpserver.service;

import org.springframework.stereotype.Service;
import se.smartairbase.mcpserver.domain.game.GameAnalyticsSnapshot;
import se.smartairbase.mcpserver.domain.game.SimulationBatch;
import se.smartairbase.mcpserver.domain.game.SimulationBatchGame;
import se.smartairbase.mcpserver.mcp.dto.CreateSimulationBatchRequestDto;
import se.smartairbase.mcpserver.mcp.dto.SimulationBatchDto;
import se.smartairbase.mcpserver.repository.GameAnalyticsSnapshotRepository;
import se.smartairbase.mcpserver.repository.SimulationBatchGameRepository;
import se.smartairbase.mcpserver.repository.SimulationBatchRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Validates, creates, and reports simulator batches while deriving result totals from analytics snapshots.
 */
@Service
public class SimulationBatchService {

    private final SimulationBatchRepository simulationBatchRepository;
    private final SimulationBatchGameRepository simulationBatchGameRepository;
    private final GameAnalyticsSnapshotRepository gameAnalyticsSnapshotRepository;
    private final SimulationBatchRunner simulationBatchRunner;

    public SimulationBatchService(SimulationBatchRepository simulationBatchRepository,
                                  SimulationBatchGameRepository simulationBatchGameRepository,
                                  GameAnalyticsSnapshotRepository gameAnalyticsSnapshotRepository,
                                  SimulationBatchRunner simulationBatchRunner) {
        this.simulationBatchRepository = simulationBatchRepository;
        this.simulationBatchGameRepository = simulationBatchGameRepository;
        this.gameAnalyticsSnapshotRepository = gameAnalyticsSnapshotRepository;
        this.simulationBatchRunner = simulationBatchRunner;
    }

    public SimulationBatchDto createBatch(CreateSimulationBatchRequestDto request) {
        String batchName = normalizeName(request.batchName());
        if (batchName.isBlank()) {
            throw new IllegalArgumentException("Simulation batch name is required");
        }
        if (simulationBatchRepository.existsByNameIgnoreCase(batchName)) {
            throw new IllegalArgumentException("The simulation batch name \"" + batchName + "\" is already in use. Choose a different name.");
        }
        int runCount = Math.max(1, request.runCount() == null ? 1 : request.runCount());
        int aircraftCount = Math.max(1, request.aircraftCount() == null ? 1 : request.aircraftCount());
        int m1Count = Math.max(0, request.missionTypeCounts() == null ? 0 : request.missionTypeCounts().getOrDefault("M1", 0));
        int m2Count = Math.max(0, request.missionTypeCounts() == null ? 0 : request.missionTypeCounts().getOrDefault("M2", 0));
        int m3Count = Math.max(0, request.missionTypeCounts() == null ? 0 : request.missionTypeCounts().getOrDefault("M3", 0));
        String diceStrategy = normalizeStrategy(request.diceStrategy());
        int maxRounds = Math.max(1, request.maxRounds() == null ? 1000 : request.maxRounds());

        SimulationBatch batch = simulationBatchRepository.save(new SimulationBatch(
                batchName,
                request.scenarioName() == null || request.scenarioName().isBlank() ? "SCN_STANDARD" : request.scenarioName(),
                aircraftCount,
                m1Count,
                m2Count,
                m3Count,
                diceStrategy,
                maxRounds,
                runCount,
                LocalDateTime.now()
        ));
        simulationBatchRunner.runBatch(batch.getId());
        return toDto(batch);
    }

    public SimulationBatchDto getBatch(Long batchId) {
        return toDto(simulationBatchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Simulation batch not found: " + batchId)));
    }

    private SimulationBatchDto toDto(SimulationBatch batch) {
        List<SimulationBatchGame> batchGames = simulationBatchGameRepository.findBySimulationBatch_IdOrderByRunNumberDesc(batch.getId());
        String currentGameName = batchGames.isEmpty() ? null : batchGames.get(0).getGameName();
        int wonRuns = 0;
        int lostRuns = 0;
        for (SimulationBatchGame batchGame : batchGames) {
            if (batchGame.getGameId() == null) {
                continue;
            }
            GameAnalyticsSnapshot snapshot = gameAnalyticsSnapshotRepository.findByGame_Id(batchGame.getGameId()).orElse(null);
            if (snapshot == null) {
                continue;
            }
            if (snapshot.isWin()) {
                wonRuns += 1;
            } else {
                lostRuns += 1;
            }
        }
        return new SimulationBatchDto(
                batch.getId(),
                batch.getName(),
                batch.getScenarioName(),
                batch.getAircraftCount(),
                batch.getM1Count(),
                batch.getM2Count(),
                batch.getM3Count(),
                batch.getDiceStrategy(),
                batch.getMaxRounds(),
                batch.getRequestedRuns(),
                batch.getCompletedRuns(),
                batch.getFailedRuns(),
                wonRuns,
                lostRuns,
                batch.getStatus().name(),
                currentGameName
        );
    }

    private String normalizeName(String value) {
        return (value == null ? "" : value.trim().toUpperCase(Locale.ROOT)).replaceAll("[^A-Z0-9_]", "");
    }

    private String normalizeStrategy(String value) {
        String normalized = value == null ? "RANDOM" : value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "MIN_DAMAGE", "MAX_DAMAGE" -> normalized;
            default -> "RANDOM";
        };
    }
}
