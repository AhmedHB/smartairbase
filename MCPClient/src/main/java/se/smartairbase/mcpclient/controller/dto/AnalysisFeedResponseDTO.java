package se.smartairbase.mcpclient.controller.dto;

import java.util.List;

/**
 * Browser-facing DTO for the analysis feed list and polling status.
 */
public record AnalysisFeedResponseDTO(
        List<AnalysisFeedItemDTO> items,
        boolean pending,
        Integer lastAnalyzedRound
) {
}
