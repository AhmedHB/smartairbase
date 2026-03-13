package se.smartairbase.mcpclient.controller.dto;

import java.util.List;

/**
 * Browser-facing DTO for the landing options available to one aircraft.
 */
public record LandingOptionsDTO(
        Long gameId,
        Integer roundNumber,
        String aircraftCode,
        boolean holdingRequired,
        List<LandingOptionDTO> options
) {
}
