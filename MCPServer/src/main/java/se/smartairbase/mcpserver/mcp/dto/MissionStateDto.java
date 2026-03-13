package se.smartairbase.mcpserver.mcp.dto;

/**
 * Read model for one mission instance in a live game.
 */
public record MissionStateDto(String code, String missionType, String status, int sortOrder, String assignmentBlocker) {
}
