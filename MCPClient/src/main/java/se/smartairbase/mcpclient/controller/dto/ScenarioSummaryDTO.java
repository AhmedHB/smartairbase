package se.smartairbase.mcpclient.controller.dto;

/**
 * Browser-facing DTO summarizing a scenario for selection lists.
 */
public record ScenarioSummaryDTO(Long scenarioId,
                                 String name,
                                 String version,
                                 String sourceType,
                                 boolean editable,
                                 boolean deletable,
                                 boolean published) {
}
