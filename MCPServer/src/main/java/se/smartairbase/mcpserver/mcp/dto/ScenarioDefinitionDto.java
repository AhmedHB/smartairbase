package se.smartairbase.mcpserver.mcp.dto;

import java.util.List;

/**
 * Carries the full editable scenario definition returned to MCP clients.
 */
public record ScenarioDefinitionDto(Long scenarioId,
                                    String name,
                                    String version,
                                    String description,
                                    String sourceType,
                                    boolean editable,
                                    boolean deletable,
                                    boolean published,
                                    List<ScenarioBaseDto> bases,
                                    List<ScenarioAircraftDto> aircraft,
                                    List<ScenarioMissionDto> missions,
                                    List<ScenarioDiceRuleDto> diceRules) {
}
