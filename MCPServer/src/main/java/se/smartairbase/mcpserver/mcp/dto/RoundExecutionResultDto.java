package se.smartairbase.mcpserver.mcp.dto;

import java.util.List;

public record RoundExecutionResultDto(Long gameId, Integer roundNumber, String gameStatus,
                                      List<String> completedMissions, List<String> aircraftUpdates,
                                      List<String> supplyDeliveries) {
}
