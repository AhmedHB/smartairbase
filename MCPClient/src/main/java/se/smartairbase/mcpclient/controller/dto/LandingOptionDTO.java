package se.smartairbase.mcpclient.controller.dto;

/**
 * Browser-facing DTO for one landing candidate in the landing dialog.
 */
public record LandingOptionDTO(
        String baseCode,
        String baseName,
        boolean canLand,
        String reason
) {
}
