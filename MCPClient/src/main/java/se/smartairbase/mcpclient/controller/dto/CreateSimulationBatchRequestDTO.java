package se.smartairbase.mcpclient.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

/**
 * HTTP request payload for starting a simulator batch from the frontend.
 */
public record CreateSimulationBatchRequestDTO(
        @NotBlank
        @Pattern(regexp = "^[A-Z0-9_]+$", message = "batchName may only contain uppercase letters, digits, and underscores")
        String batchName,
        @NotBlank String scenarioName,
        @Min(1) Integer aircraftCount,
        Map<String, Integer> missionTypeCounts,
        String diceStrategy,
        @Min(1) Integer runCount,
        @Min(1) Integer maxRounds
) {
}
