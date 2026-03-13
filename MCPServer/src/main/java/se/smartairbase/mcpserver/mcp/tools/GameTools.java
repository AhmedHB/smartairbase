package se.smartairbase.mcpserver.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import se.smartairbase.mcpserver.mcp.dto.AnalysisFeedItemDto;
import se.smartairbase.mcpserver.service.AnalysisFeedPersistenceService;
import se.smartairbase.mcpserver.service.GameAnalyticsQueryService;
import se.smartairbase.mcpserver.service.GameService;
import se.smartairbase.mcpserver.service.GameQueryService;
import se.smartairbase.mcpserver.service.ScenarioService;
import se.smartairbase.mcpserver.service.SimulationBatchService;

import java.util.List;
import java.util.Map;

@Component
/**
 * Exposes game lifecycle, analytics, scenario, and simulation operations as MCP tools.
 */
public class GameTools {

    private final GameService gameService;
    private final GameQueryService gameQueryService;
    private final AnalysisFeedPersistenceService analysisFeedPersistenceService;
    private final GameAnalyticsQueryService gameAnalyticsQueryService;
    private final ScenarioService scenarioService;
    private final SimulationBatchService simulationBatchService;

    public GameTools(GameService gameService,
                     GameQueryService gameQueryService,
                     AnalysisFeedPersistenceService analysisFeedPersistenceService,
                     GameAnalyticsQueryService gameAnalyticsQueryService,
                     ScenarioService scenarioService,
                     SimulationBatchService simulationBatchService) {
        this.gameService = gameService;
        this.gameQueryService = gameQueryService;
        this.analysisFeedPersistenceService = analysisFeedPersistenceService;
        this.gameAnalyticsQueryService = gameAnalyticsQueryService;
        this.scenarioService = scenarioService;
        this.simulationBatchService = simulationBatchService;
    }

    @Tool(
            name = "create_game",
            description = "Create a new game from a scenario, optionally with a unique custom game name"
    )
    public Object createGame(String scenarioName,
                             String version,
                             String gameName,
                             Integer aircraftCount,
                             Map<String, Integer> missionTypeCounts,
                             Integer maxRounds) {
        return gameService.createGameFromScenario(scenarioName, version, gameName, aircraftCount, missionTypeCounts, maxRounds);
    }

    @Tool(
            name = "get_game_state",
            description = "Get the current state of a game"
    )
    public Object getGameState(Long gameId) {
        return gameQueryService.getGameState(gameId);
    }

    @Tool(
            name = "list_scenarios",
            description = "List available scenarios, including the protected standard scenario and any user-created copies"
    )
    public Object listScenarios() {
        return scenarioService.listScenarios();
    }

    @Tool(
            name = "get_scenario",
            description = "Get one scenario definition with bases, aircraft, missions, deliveries and dice rules"
    )
    public Object getScenario(Long scenarioId) {
        return scenarioService.getScenario(scenarioId);
    }

    @Tool(
            name = "duplicate_scenario",
            description = "Duplicate one existing scenario into a user-editable copy"
    )
    public Object duplicateScenario(Long scenarioId, String name) {
        return scenarioService.duplicateScenario(scenarioId, name);
    }

    @Tool(
            name = "update_scenario",
            description = "Update one user-created scenario by changing existing base capacities, aircraft settings and mission costs"
    )
    public Object updateScenario(Long scenarioId,
                                 List<Map<String, Object>> bases,
                                 List<Map<String, Object>> aircraft,
                                 List<Map<String, Object>> missions,
                                 String description) {
        return scenarioService.updateScenario(
                scenarioId,
                ScenarioToolMapper.toUpdateScenarioRequest(bases, aircraft, missions, description)
        );
    }

    @Tool(
            name = "delete_scenario",
            description = "Delete one user-created scenario; protected system scenarios cannot be deleted"
    )
    public Object deleteScenario(Long scenarioId) {
        return scenarioService.deleteScenario(scenarioId);
    }

    @Tool(
            name = "create_game_from_scenario",
            description = "Create a new game directly from a selected scenario"
    )
    public Object createGameFromScenario(Long scenarioId, String gameName) {
        return scenarioService.createGameFromScenario(scenarioId, gameName);
    }

    @Tool(
            name = "create_simulation_batch",
            description = "Create one saved simulation batch that runs many games without analysis feed playback"
    )
    public Object createSimulationBatch(String batchName,
                                        String scenarioName,
                                        Integer aircraftCount,
                                        Map<String, Integer> missionTypeCounts,
                                        String diceStrategy,
                                        Integer runCount,
                                        Integer maxRounds) {
        return simulationBatchService.createBatch(new se.smartairbase.mcpserver.mcp.dto.CreateSimulationBatchRequestDto(
                batchName,
                scenarioName,
                aircraftCount,
                missionTypeCounts,
                diceStrategy,
                runCount,
                maxRounds
        ));
    }

    @Tool(
            name = "get_simulation_batch",
            description = "Get progress for one saved simulation batch"
    )
    public Object getSimulationBatch(Long simulationBatchId) {
        return simulationBatchService.getBatch(simulationBatchId);
    }

    @Tool(
            name = "list_game_analytics_snapshots",
            description = "List finished-game analytics rows, newest first, optionally filtered by scenario, date, aircraft count, and mission counts"
    )
    public Object listGameAnalyticsSnapshots(String scenarioName,
                                             String createdDate,
                                             Integer aircraftCount,
                                             Integer m1Count,
                                             Integer m2Count,
                                             Integer m3Count) {
        return gameAnalyticsQueryService.listSnapshots(scenarioName, createdDate, aircraftCount, m1Count, m2Count, m3Count);
    }

    @Tool(
            name = "abort_game",
            description = "Abort one game and make it inactive for further play"
    )
    public Object abortGame(Long gameId) {
        return gameService.abortGame(gameId);
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
