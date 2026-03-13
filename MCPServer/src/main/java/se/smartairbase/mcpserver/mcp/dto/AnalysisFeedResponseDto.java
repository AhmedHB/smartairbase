package se.smartairbase.mcpserver.mcp.dto;

import java.util.List;

/**
 * Wrapper around the persisted analysis-feed history for one game.
 */
public record AnalysisFeedResponseDto(
        List<AnalysisFeedItemDto> items,
        boolean pending,
        Integer lastAnalyzedRound
) {
}
