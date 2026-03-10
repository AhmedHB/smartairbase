package se.smartairbase.mcpclient.service.model;

public record LandingOptionView(
        String baseCode,
        String baseName,
        boolean canLand,
        String reason
) {
}
