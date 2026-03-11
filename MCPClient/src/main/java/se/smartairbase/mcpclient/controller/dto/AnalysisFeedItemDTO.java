package se.smartairbase.mcpclient.controller.dto;

import java.util.List;

public record AnalysisFeedItemDTO(
        String id,
        Long gameId,
        Integer round,
        String phase,
        String role,
        String summary,
        String details,
        List<String> relatedAircraft,
        List<String> relatedBases,
        String createdAt
) {
}
