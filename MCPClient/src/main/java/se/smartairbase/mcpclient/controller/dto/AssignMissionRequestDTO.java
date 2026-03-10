package se.smartairbase.mcpclient.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignMissionRequestDTO(
        @NotBlank String aircraftCode,
        @NotBlank String missionCode
) {
}
