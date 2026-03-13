package se.smartairbase.mcpserver.mcp.dto;

import java.util.List;

/**
 * Carries editable scenario values submitted from clients.
 */
public record UpdateScenarioRequestDto(String description,
                                       List<ScenarioBaseDto> bases,
                                       List<ScenarioAircraftDto> aircraft,
                                       List<ScenarioMissionDto> missions) {
}
