package se.smartairbase.mcpserver.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import se.smartairbase.mcpserver.mcp.dto.AnalysisFeedItemDto;
import se.smartairbase.mcpserver.service.AnalysisFeedPersistenceService;
import se.smartairbase.mcpserver.service.GameService;
import se.smartairbase.mcpserver.service.GameQueryService;

import java.util.List;
import java.util.Map;

@Component
public class GameTools {

    private final GameService gameService;
    private final GameQueryService gameQueryService;
    private final AnalysisFeedPersistenceService analysisFeedPersistenceService;

    public GameTools(GameService gameService,
                     GameQueryService gameQueryService,
                     AnalysisFeedPersistenceService analysisFeedPersistenceService) {
        this.gameService = gameService;
        this.gameQueryService = gameQueryService;
        this.analysisFeedPersistenceService = analysisFeedPersistenceService;
    }

    @Tool(
            name = "create_game",
            description = "Create a new game from a scenario"
    )
    public Object createGame(String scenarioName,
                             String version,
                             Integer aircraftCount,
                             Map<String, Integer> missionTypeCounts) {
        return gameService.createGameFromScenario(scenarioName, version, null, aircraftCount, missionTypeCounts);
    }

    @Tool(
            name = "get_game_state",
            description = "Get the current state of a game"
    )
    public Object getGameState(Long gameId) {
        return gameQueryService.getGameState(gameId);
    }

    @Tool(
            name = "list_analysis_feed",
            description = "List the saved analysis feed entries for one game"
    )
    public Object listAnalysisFeed(Long gameId) {
        return analysisFeedPersistenceService.listFeed(gameId);
    }

    @Tool(
            name = "append_analysis_feed_items",
            description = "Append analysis feed entries to one game and return the full saved feed"
    )
    public Object appendAnalysisFeedItems(Long gameId, List<AnalysisFeedItemDto> items) {
        return analysisFeedPersistenceService.appendFeedItems(gameId, items);
    }
}
