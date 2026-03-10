package se.smartairbase.mcpclient.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateGameRequest(
        @NotBlank String scenarioName,
        @Pattern(regexp = "\\d+", message = "version must be numeric") String version
) {
}
