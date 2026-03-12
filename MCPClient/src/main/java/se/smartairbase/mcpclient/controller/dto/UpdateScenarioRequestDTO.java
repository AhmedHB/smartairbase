package se.smartairbase.mcpclient.controller.dto;

import java.util.List;

public record UpdateScenarioRequestDTO(String description,
                                       List<ScenarioBaseDTO> bases,
                                       List<ScenarioAircraftDTO> aircraft,
                                       List<ScenarioMissionDTO> missions) {
}
