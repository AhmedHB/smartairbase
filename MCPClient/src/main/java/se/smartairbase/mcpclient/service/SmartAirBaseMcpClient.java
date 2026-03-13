package se.smartairbase.mcpclient.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import se.smartairbase.mcpclient.controller.dto.ActionResultDTO;
import se.smartairbase.mcpclient.controller.dto.AircraftStateDTO;
import se.smartairbase.mcpclient.controller.dto.AnalysisFeedItemDTO;
import se.smartairbase.mcpclient.controller.dto.AnalysisFeedResponseDTO;
import se.smartairbase.mcpclient.domain.SmartAirBaseTool;
import se.smartairbase.mcpclient.controller.dto.AssignMissionRequestDTO;
import se.smartairbase.mcpclient.controller.dto.CreateScenarioGameRequestDTO;
import se.smartairbase.mcpclient.controller.dto.CreateGameRequestDTO;
import se.smartairbase.mcpclient.controller.dto.DiceRollRequestDTO;
import se.smartairbase.mcpclient.controller.dto.DuplicateScenarioRequestDTO;
import se.smartairbase.mcpclient.controller.dto.BaseStateDTO;
import se.smartairbase.mcpclient.controller.dto.GameStateDTO;
import se.smartairbase.mcpclient.controller.dto.GameSummaryDTO;
import se.smartairbase.mcpclient.controller.dto.LandAircraftRequestDTO;
import se.smartairbase.mcpclient.controller.dto.LandingOptionsDTO;
import se.smartairbase.mcpclient.controller.dto.RoundExecutionResultDTO;
import se.smartairbase.mcpclient.controller.dto.ScenarioDefinitionDTO;
import se.smartairbase.mcpclient.controller.dto.ScenarioSummaryDTO;
import se.smartairbase.mcpclient.controller.dto.UpdateScenarioRequestDTO;

import java.util.Map;
import java.util.List;

/**
 * Typed facade over the Smart Air Base MCP tool set.
 *
 * <p>This class keeps all request-shape knowledge in one place so controllers and
 * UI code do not need to know how each MCP tool expects its payload.</p>
 */
@Service
public class SmartAirBaseMcpClient {

    private final McpToolExecutor toolExecutor;
    private final ObjectMapper objectMapper;

    public SmartAirBaseMcpClient(McpToolExecutor toolExecutor, ObjectMapper objectMapper) {
        this.toolExecutor = toolExecutor;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates a new game from a seeded scenario on the MCP server.
     */
    public GameSummaryDTO createGame(CreateGameRequestDTO request) {
        return toolExecutor.execute(SmartAirBaseTool.CREATE_GAME, request, GameSummaryDTO.class);
    }

    public GameSummaryDTO createGameFromScenario(String scenarioId, CreateScenarioGameRequestDTO request) {
        return toolExecutor.execute(SmartAirBaseTool.CREATE_GAME_FROM_SCENARIO, Map.of(
                "scenarioId", scenarioId,
                "gameName", request != null ? request.gameName() : null
        ), GameSummaryDTO.class);
    }

    public List<ScenarioSummaryDTO> listScenarios() {
        return objectMapper.convertValue(
                toolExecutor.execute(SmartAirBaseTool.LIST_SCENARIOS, Map.of(), Object.class),
                objectMapper.getTypeFactory().constructCollectionType(List.class, ScenarioSummaryDTO.class)
        );
    }

    public ScenarioDefinitionDTO getScenario(String scenarioId) {
        return toolExecutor.execute(SmartAirBaseTool.GET_SCENARIO, Map.of("scenarioId", scenarioId), ScenarioDefinitionDTO.class);
    }

    public ScenarioDefinitionDTO duplicateScenario(String scenarioId, DuplicateScenarioRequestDTO request) {
        return toolExecutor.execute(SmartAirBaseTool.DUPLICATE_SCENARIO, Map.of(
                "scenarioId", scenarioId,
                "name", request.name()
        ), ScenarioDefinitionDTO.class);
    }

    public ScenarioDefinitionDTO updateScenario(String scenarioId, UpdateScenarioRequestDTO request) {
        return toolExecutor.execute(SmartAirBaseTool.UPDATE_SCENARIO, Map.of(
                "scenarioId", scenarioId,
                "description", request.description(),
                "bases", request.bases(),
                "aircraft", request.aircraft(),
                "missions", request.missions()
        ), ScenarioDefinitionDTO.class);
    }

    public ActionResultDTO deleteScenario(String scenarioId) {
        return toolExecutor.execute(SmartAirBaseTool.DELETE_SCENARIO, Map.of("scenarioId", scenarioId), ActionResultDTO.class);
    }

    /**
     * Returns the aggregated state view for one game.
     */
    public GameStateDTO getGameState(String gameId) {
        return toolExecutor.execute(SmartAirBaseTool.GET_GAME_STATE, Map.of("gameId", gameId), GameStateDTO.class);
    }

    public GameStateDTO getGameStateView(String gameId) {
        return getGameState(gameId);
    }

    /**
     * Aborts one game on the MCP server and returns the resulting action status.
     */
    public ActionResultDTO abortGame(String gameId) {
        return toolExecutor.execute(SmartAirBaseTool.ABORT_GAME, Map.of("gameId", gameId), ActionResultDTO.class);
    }

    public AnalysisFeedResponseDTO listAnalysisFeed(String gameId) {
        return toolExecutor.execute(SmartAirBaseTool.LIST_ANALYSIS_FEED, Map.of("gameId", gameId), AnalysisFeedResponseDTO.class);
    }

    public AnalysisFeedResponseDTO appendAnalysisFeedItems(String gameId, List<AnalysisFeedItemDTO> items) {
        return toolExecutor.execute(
                SmartAirBaseTool.APPEND_ANALYSIS_FEED_ITEMS,
                Map.of("gameId", gameId, "items", items),
                AnalysisFeedResponseDTO.class
        );
    }

    /**
     * Assigns a mission to one aircraft during the planning phase.
     */
    public ActionResultDTO assignMission(String gameId, AssignMissionRequestDTO request) {
        return toolExecutor.execute(SmartAirBaseTool.ASSIGN_MISSION, Map.of(
                "gameId", gameId,
                "aircraftCode", request.aircraftCode(),
                "missionCode", request.missionCode()
        ), ActionResultDTO.class);
    }

    /**
     * Opens a new round.
     */
    public RoundExecutionResultDTO startRound(String gameId) {
        return toolExecutor.execute(SmartAirBaseTool.START_ROUND, Map.of("gameId", gameId), RoundExecutionResultDTO.class);
    }

    /**
     * Resolves all assigned missions and moves the round into dice handling.
     */
    public RoundExecutionResultDTO resolveMissions(String gameId) {
        return toolExecutor.execute(SmartAirBaseTool.RESOLVE_MISSIONS, Map.of("gameId", gameId), RoundExecutionResultDTO.class);
    }

    /**
     * Stores one resolved dice value together with the mode that produced it.
     */
    public ActionResultDTO recordDiceRoll(String gameId, DiceRollRequestDTO request) {
        return toolExecutor.execute(SmartAirBaseTool.RECORD_DICE_ROLL, Map.of(
                "gameId", gameId,
                "aircraftCode", request.aircraftCode(),
                "diceValue", request.diceValue(),
                "diceSelectionMode", request.diceSelectionMode()
        ), ActionResultDTO.class);
    }

    /**
     * Lists valid landing targets after damage resolution.
     */
    public LandingOptionsDTO listAvailableLandingBases(String gameId, String aircraftCode) {
        return toolExecutor.execute(SmartAirBaseTool.LIST_AVAILABLE_LANDING_BASES, Map.of(
                "gameId", gameId,
                "aircraftCode", aircraftCode
        ), LandingOptionsDTO.class);
    }

    public LandingOptionsDTO getLandingOptionsView(String gameId, String aircraftCode) {
        return listAvailableLandingBases(gameId, aircraftCode);
    }

    /**
     * Lands an aircraft at the chosen base.
     */
    public ActionResultDTO landAircraft(String gameId, LandAircraftRequestDTO request) {
        return toolExecutor.execute(SmartAirBaseTool.LAND_AIRCRAFT, Map.of(
                "gameId", gameId,
                "aircraftCode", request.aircraftCode(),
                "baseCode", request.baseCode()
        ), ActionResultDTO.class);
    }

    /**
     * Sends an aircraft to holding when no base can receive it.
     */
    public ActionResultDTO sendAircraftToHolding(String gameId, String aircraftCode) {
        return toolExecutor.execute(SmartAirBaseTool.SEND_AIRCRAFT_TO_HOLDING, Map.of(
                "gameId", gameId,
                "aircraftCode", aircraftCode
        ), ActionResultDTO.class);
    }

    /**
     * Finalizes the round and applies end-of-round effects.
     */
    public RoundExecutionResultDTO completeRound(String gameId) {
        return toolExecutor.execute(SmartAirBaseTool.COMPLETE_ROUND, Map.of("gameId", gameId), RoundExecutionResultDTO.class);
    }

    /**
     * Returns the state of one aircraft.
     */
    public AircraftStateDTO getAircraftState(String gameId, String aircraftCode) {
        return toolExecutor.execute(SmartAirBaseTool.GET_AIRCRAFT_STATE, Map.of(
                "gameId", gameId,
                "aircraftCode", aircraftCode
        ), AircraftStateDTO.class);
    }

    /**
     * Returns the state of one base.
     */
    public BaseStateDTO getBaseState(String gameId, String baseCode) {
        return toolExecutor.execute(SmartAirBaseTool.GET_BASE_STATE, Map.of(
                "gameId", gameId,
                "baseCode", baseCode
        ), BaseStateDTO.class);
    }
}
