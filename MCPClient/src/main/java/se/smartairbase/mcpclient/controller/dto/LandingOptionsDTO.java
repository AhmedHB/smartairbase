package se.smartairbase.mcpclient.controller.dto;

import java.util.List;

public record LandingOptionsDTO(
        Long gameId,
        Integer roundNumber,
        String aircraftCode,
        boolean holdingRequired,
        List<LandingOptionDTO> options
) {
}
