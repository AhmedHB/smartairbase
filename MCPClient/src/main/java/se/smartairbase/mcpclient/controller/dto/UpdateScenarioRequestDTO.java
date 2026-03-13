package se.smartairbase.mcpclient.controller.dto;

import java.util.List;

/**
 * HTTP request payload for saving editable scenario values.
 */
public record UpdateScenarioRequestDTO(String description,
                                       List<ScenarioBaseDTO> bases,
                                       List<ScenarioAircraftDTO> aircraft,
                                       List<ScenarioMissionDTO> missions) {
}
