package se.smartairbase.mcpserver.mcp.dto;

import java.util.List;

public record RoundExecutionResultDto(Long gameId, Integer roundNumber, String phase, String gameStatus,
                                      boolean roundOpen, List<String> pendingAircraft,
                                      List<String> affectedAircraft, List<String> completedMissions,
                                      List<String> supplyDeliveries, List<String> messages) {
}
