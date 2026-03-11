package se.smartairbase.mcpserver.mcp.dto;

import java.util.List;

public record AnalysisFeedResponseDto(
        List<AnalysisFeedItemDto> items,
        boolean pending,
        Integer lastAnalyzedRound
) {
}
