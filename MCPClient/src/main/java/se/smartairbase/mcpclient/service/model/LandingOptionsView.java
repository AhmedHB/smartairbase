package se.smartairbase.mcpclient.service.model;

import java.util.List;

public record LandingOptionsView(
        Long gameId,
        Integer roundNumber,
        String aircraftCode,
        boolean holdingRequired,
        List<LandingOptionView> options
) {
}
