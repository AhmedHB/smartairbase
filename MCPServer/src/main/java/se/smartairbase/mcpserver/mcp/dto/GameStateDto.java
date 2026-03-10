package se.smartairbase.mcpserver.mcp.dto;

import java.util.List;

public record GameStateDto(GameSummaryDto game, List<BaseStateDto> bases, List<AircraftStateDto> aircraft,
                           List<MissionStateDto> missions, long eventCount, long transactionCount) {
}
