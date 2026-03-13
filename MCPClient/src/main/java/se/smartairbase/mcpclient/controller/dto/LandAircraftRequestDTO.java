package se.smartairbase.mcpclient.controller.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * HTTP request payload for landing an aircraft at a chosen base.
 */
public record LandAircraftRequestDTO(
        @NotBlank String aircraftCode,
        @NotBlank String baseCode
) {
}
