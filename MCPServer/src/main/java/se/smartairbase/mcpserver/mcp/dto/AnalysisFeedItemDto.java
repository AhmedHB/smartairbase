package se.smartairbase.mcpserver.mcp.dto;

import java.util.List;

/**
 * Serialized analysis-feed entry passed between MCPClient and MCPServer.
 */
public record AnalysisFeedItemDto(
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
