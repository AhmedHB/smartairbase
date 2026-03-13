package se.smartairbase.mcpclient.controller.dto;

/**
 * HTTP response payload exposing simulator batch progress and summarized outcomes to the frontend.
 */
public record SimulationBatchDTO(
        Long simulationBatchId,
        String name,
        String scenarioName,
        Integer aircraftCount,
        Integer m1Count,
        Integer m2Count,
        Integer m3Count,
        String diceStrategy,
        Integer maxRounds,
        Integer requestedRuns,
        Integer completedRuns,
        Integer failedRuns,
        Integer wonRuns,
        Integer lostRuns,
        String status,
        String currentGameName
) {
}
