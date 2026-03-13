package se.smartairbase.mcpclient.controller.dto;

/**
 * Browser-facing DTO for one supply delivery rule attached to a scenario base.
 */
public record ScenarioSupplyRuleDTO(String resource, Integer frequencyRounds, Integer deliveryAmount) {
}
