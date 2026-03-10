package se.smartairbase.mcpclient.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record LandAircraftRequest(
        @NotBlank String aircraftCode,
        @NotBlank String baseCode
) {
}
