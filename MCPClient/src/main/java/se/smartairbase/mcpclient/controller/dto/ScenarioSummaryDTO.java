package se.smartairbase.mcpclient.controller.dto;

public record ScenarioSummaryDTO(Long scenarioId,
                                 String name,
                                 String version,
                                 String sourceType,
                                 boolean editable,
                                 boolean deletable,
                                 boolean published) {
}
