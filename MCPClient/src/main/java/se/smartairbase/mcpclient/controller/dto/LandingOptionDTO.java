package se.smartairbase.mcpclient.controller.dto;

public record LandingOptionDTO(
        String baseCode,
        String baseName,
        boolean canLand,
        String reason
) {
}
