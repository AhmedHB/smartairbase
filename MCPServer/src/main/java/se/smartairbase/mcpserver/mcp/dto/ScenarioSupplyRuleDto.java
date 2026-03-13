package se.smartairbase.mcpserver.mcp.dto;

/**
 * Describes one supply delivery rule attached to a scenario base.
 */
public record ScenarioSupplyRuleDto(String resource, Integer frequencyRounds, Integer deliveryAmount) {
}
