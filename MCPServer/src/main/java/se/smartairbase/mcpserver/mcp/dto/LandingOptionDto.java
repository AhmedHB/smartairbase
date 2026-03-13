package se.smartairbase.mcpserver.mcp.dto;

/**
 * One candidate landing base with acceptance result and optional blocker reason.
 */
public record LandingOptionDto(String baseCode, String baseName, boolean canLand, String reason) {
}
