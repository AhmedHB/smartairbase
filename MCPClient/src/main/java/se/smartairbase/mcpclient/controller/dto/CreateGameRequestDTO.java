package se.smartairbase.mcpclient.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

/**
 * HTTP request payload for creating one playable game from a scenario, with optional naming and round-limit overrides.
 */
public record CreateGameRequestDTO(
        @NotBlank String scenarioName,
        @Pattern(regexp = "(?i)v?\\d+", message = "version must be numeric, with optional V prefix") String version,
        @Pattern(regexp = "^(?!\\s*$).+", message = "gameName must not be blank") String gameName,
        Integer aircraftCount,
        Map<String, Integer> missionTypeCounts,
        @Min(1) Integer maxRounds
) {
}
