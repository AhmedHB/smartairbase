package se.smartairbase.mcpclient.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record LandAircraftRequestDTO(
        @NotBlank String aircraftCode,
        @NotBlank String baseCode
) {
}
