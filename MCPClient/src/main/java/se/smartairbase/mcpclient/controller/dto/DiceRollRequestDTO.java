package se.smartairbase.mcpclient.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Payload for one resolved dice roll.
 *
 * <p>The selection mode is validated explicitly because the server persists it
 * per roll and later derives game-level analytics from the collected set of
 * recorded rolls.</p>
 */
public record DiceRollRequestDTO(
        @NotBlank String aircraftCode,
        @Min(1) @Max(6) int diceValue,
        @NotBlank
        @Pattern(
                regexp = "^(MANUAL_DIRECT_SELECTION|MANUAL_RANDOM_SELECTION|AUTO_RANDOM|AUTO_MIN_DAMAGE|AUTO_MAX_DAMAGE)$",
                message = "diceSelectionMode must be a supported selection mode"
        ) String diceSelectionMode
) {
}
