package se.smartairbase.mcpserver.mcp.dto;

/**
 * Returns the current state and aggregate results for one simulator batch.
 */
public record SimulationBatchDto(
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
