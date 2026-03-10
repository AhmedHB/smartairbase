package se.smartairbase.mcpserver.mcp.dto;

public record AssignMissionRequest(Long gameId, String aircraftCode, String missionCode) {
}
