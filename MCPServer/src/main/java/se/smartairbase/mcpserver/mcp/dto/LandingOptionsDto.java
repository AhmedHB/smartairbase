package se.smartairbase.mcpserver.mcp.dto;

import java.util.List;

public record LandingOptionsDto(Long gameId, Integer roundNumber, String aircraftCode,
                                boolean holdingRequired, List<LandingOptionDto> options) {
}
