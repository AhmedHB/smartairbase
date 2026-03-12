package se.smartairbase.mcpclient.controller.dto;

import java.util.List;

public record ScenarioDefinitionDTO(Long scenarioId,
                                    String name,
                                    String version,
                                    String description,
                                    String sourceType,
                                    boolean editable,
                                    boolean deletable,
                                    boolean published,
                                    List<ScenarioBaseDTO> bases,
                                    List<ScenarioAircraftDTO> aircraft,
                                    List<ScenarioMissionDTO> missions,
                                    List<ScenarioDiceRuleDTO> diceRules) {
}
