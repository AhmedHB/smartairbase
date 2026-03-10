package se.smartairbase.mcpserver.mcp.dto;

public record LandingOptionDto(String baseCode, String baseName, boolean canLand, String reason) {
}
