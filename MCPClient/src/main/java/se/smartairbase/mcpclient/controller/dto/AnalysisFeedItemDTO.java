package se.smartairbase.mcpclient.controller.dto;

import java.util.List;

/**
 * Browser-facing DTO for one persisted analysis feed entry.
 */
public record AnalysisFeedItemDTO(
        String id,
        Long gameId,
        Integer round,
        String phase,
        String role,
        String source,
        String summary,
        String details,
        List<String> relatedAircraft,
        List<String> relatedBases,
        String createdAt
) {
}
