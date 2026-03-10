package se.smartairbase.mcpserver.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import se.smartairbase.mcpserver.service.GameService;
import se.smartairbase.mcpserver.service.GameQueryService;

import java.util.Map;

@Component
public class GameTools {

    private final GameService gameService;
    private final GameQueryService gameQueryService;

    public GameTools(GameService gameService,
                     GameQueryService gameQueryService) {
        this.gameService = gameService;
        this.gameQueryService = gameQueryService;
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
}
