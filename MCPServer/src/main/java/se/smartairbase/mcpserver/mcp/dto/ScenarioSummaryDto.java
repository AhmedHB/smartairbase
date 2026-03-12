package se.smartairbase.mcpserver.mcp.dto;

public record ScenarioSummaryDto(Long scenarioId,
                                 String name,
                                 String version,
                                 String sourceType,
                                 boolean editable,
                                 boolean deletable,
                                 boolean published) {
}
