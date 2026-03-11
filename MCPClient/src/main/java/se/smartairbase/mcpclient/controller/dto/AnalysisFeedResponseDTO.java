package se.smartairbase.mcpclient.controller.dto;

import java.util.List;

public record AnalysisFeedResponseDTO(
        List<AnalysisFeedItemDTO> items,
        boolean pending,
        Integer lastAnalyzedRound
) {
}
