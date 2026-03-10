package se.smartairbase.mcpserver.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import se.smartairbase.mcpserver.mcp.dto.*;
import se.smartairbase.mcpserver.service.GameQueryService;
import se.smartairbase.mcpserver.service.GameService;
import se.smartairbase.mcpserver.service.RoundService;

// @Component
public class SmartAirBaseTools {

    private final GameService gameService;
    private final GameQueryService gameQueryService;
    private final RoundService roundService;

    public SmartAirBaseTools(GameService gameService, GameQueryService gameQueryService, RoundService roundService) {
        this.gameService = gameService;
        this.gameQueryService = gameQueryService;
        this.roundService = roundService;
    }

    @Tool(description = "Create a new SmartAirBase game from a scenario name and version.")
    public GameSummaryDto createGame(CreateGameRequest request) {
        return gameService.createGameFromScenario(request.scenarioName(), request.version(), request.gameName());
    }

    @Tool(description = "Get the current state of a SmartAirBase game, including bases, aircraft, and missions.")
    public GameStateDto getGameState(Long gameId) {
        return gameQueryService.getGameState(gameId);
    }

    @Tool(description = "Assign an available mission to a ready aircraft in a SmartAirBase game.")
    public ActionResultDto assignMission(AssignMissionRequest request) {
        return roundService.assignMission(request.gameId(), request.aircraftCode(), request.missionCode());
    }

    @Tool(description = "Execute the next round for a SmartAirBase game, including mission resolution, dice rolls, maintenance progress, and supply deliveries.")
    public RoundExecutionResultDto executeRound(ExecuteRoundRequest request) {
        return roundService.executeRound(request.gameId());
    }
}
