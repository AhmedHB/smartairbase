package se.smartairbase.mcpclient.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

public record CreateGameRequestDTO(
        @NotBlank String scenarioName,
        @Pattern(regexp = "(?i)v?\\d+", message = "version must be numeric, with optional V prefix") String version,
        Integer aircraftCount,
        Map<String, Integer> missionTypeCounts
) {
}
