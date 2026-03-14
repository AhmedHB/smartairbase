package se.smartairbase.mcpclient.controller.dto;

import java.util.List;

/**
 * Browser-facing DTO combining the analytics snapshot and the final per-role narration for a finished game.
 */
public record GameSummaryResponseDTO(
        GameAnalyticsSnapshotDTO snapshot,
        List<AnalysisFeedItemDTO> finalFeed
) {
}
