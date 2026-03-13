package se.smartairbase.mcpclient.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * HTTP request payload for duplicating a scenario under a new name.
 */
public record DuplicateScenarioRequestDTO(
        @NotBlank
        @Pattern(regexp = "^[A-Z0-9_]+$", message = "name must contain only uppercase letters, digits, and underscores")
        String name) {
}
