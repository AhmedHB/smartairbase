package se.smartairbase.mcpserver.mcp.dto;

import java.util.List;

/**
 * Full read model for one live game, combining game, bases, aircraft, and missions.
 */
public record GameStateDto(GameSummaryDto game, List<BaseStateDto> bases, List<AircraftStateDto> aircraft,
                           List<MissionStateDto> missions, long eventCount, long transactionCount) {
}
