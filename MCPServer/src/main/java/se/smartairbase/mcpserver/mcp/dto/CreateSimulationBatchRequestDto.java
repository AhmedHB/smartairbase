package se.smartairbase.mcpserver.mcp.dto;

import java.util.Map;

/**
 * Carries simulator batch input from MCP tools into the server-side simulation services.
 */
public record CreateSimulationBatchRequestDto(
        String batchName,
        String scenarioName,
        Integer aircraftCount,
        Map<String, Integer> missionTypeCounts,
        String diceStrategy,
        Integer runCount,
        Integer maxRounds
) {
}
