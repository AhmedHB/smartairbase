package se.smartairbase.mcpserver.mcp.dto;

/**
 * Minimal success/failure response for one game action.
 */
public record ActionResultDto(boolean success, String message) {
}
