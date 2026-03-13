package se.smartairbase.mcpserver.mcp.dto;

import java.util.List;

/**
 * Landing-choice response for one aircraft in the landing phase.
 */
public record LandingOptionsDto(Long gameId, Integer roundNumber, String aircraftCode,
                                boolean holdingRequired, List<LandingOptionDto> options) {
}
