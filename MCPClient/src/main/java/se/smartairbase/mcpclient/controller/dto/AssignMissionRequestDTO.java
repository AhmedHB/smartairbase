package se.smartairbase.mcpclient.controller.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * HTTP request payload for assigning one aircraft to one mission.
 */
public record AssignMissionRequestDTO(
        @NotBlank String aircraftCode,
        @NotBlank String missionCode
) {
}
