package se.smartairbase.mcpclient.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record DiceRollRequest(
        @NotBlank String aircraftCode,
        @Min(1) @Max(6) int diceValue
) {
}
