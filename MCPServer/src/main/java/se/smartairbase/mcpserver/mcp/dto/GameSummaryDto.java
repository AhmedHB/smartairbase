package se.smartairbase.mcpserver.mcp.dto;

public record GameSummaryDto(Long gameId, String name, String scenarioName, String scenarioVersion, String status, Integer currentRound) {
}
