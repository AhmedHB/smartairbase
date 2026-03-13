package se.smartairbase.mcpserver.mcp.dto;

/**
 * Summarizes a scenario for list views and selection controls.
 */
public record ScenarioSummaryDto(Long scenarioId,
                                 String name,
                                 String version,
                                 String sourceType,
                                 boolean editable,
                                 boolean deletable,
                                 boolean published) {
}
