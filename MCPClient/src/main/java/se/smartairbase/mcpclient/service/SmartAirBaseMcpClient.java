package se.smartairbase.mcpclient.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import se.smartairbase.mcpclient.domain.SmartAirBaseTool;
import se.smartairbase.mcpclient.controller.dto.AssignMissionRequest;
import se.smartairbase.mcpclient.controller.dto.CreateGameRequest;
import se.smartairbase.mcpclient.controller.dto.DiceRollRequest;
import se.smartairbase.mcpclient.controller.dto.LandAircraftRequest;
import se.smartairbase.mcpclient.service.model.GameStateView;
import se.smartairbase.mcpclient.service.model.LandingOptionsView;

import java.util.Map;

@Service
/**
 * Typed facade over the Smart Air Base MCP tool set.
 *
 * <p>This class keeps all request-shape knowledge in one place so controllers and
 * UI code do not need to know how each MCP tool expects its payload.</p>
 */
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
    public JsonNode createGame(CreateGameRequest request) {
        return toolExecutor.execute(SmartAirBaseTool.CREATE_GAME, request);
    }

    /**
     * Returns the aggregated state view for one game.
     */
    public JsonNode getGameState(String gameId) {
        return toolExecutor.execute(SmartAirBaseTool.GET_GAME_STATE, Map.of("gameId", gameId));
    }

    public GameStateView getGameStateView(String gameId) {
        return objectMapper.convertValue(getGameState(gameId), GameStateView.class);
    }

    /**
     * Assigns a mission to one aircraft during the planning phase.
     */
    public JsonNode assignMission(String gameId, AssignMissionRequest request) {
        return toolExecutor.execute(SmartAirBaseTool.ASSIGN_MISSION, Map.of(
                "gameId", gameId,
                "aircraftCode", request.aircraftCode(),
                "missionCode", request.missionCode()
        ));
    }

    /**
     * Opens a new round.
     */
    public JsonNode startRound(String gameId) {
        return toolExecutor.execute(SmartAirBaseTool.START_ROUND, Map.of("gameId", gameId));
    }

    /**
     * Resolves all assigned missions and moves the round into dice handling.
     */
    public JsonNode resolveMissions(String gameId) {
        return toolExecutor.execute(SmartAirBaseTool.RESOLVE_MISSIONS, Map.of("gameId", gameId));
    }

    /**
     * Stores the player's dice value for one aircraft.
     */
    public JsonNode recordDiceRoll(String gameId, DiceRollRequest request) {
        return toolExecutor.execute(SmartAirBaseTool.RECORD_DICE_ROLL, Map.of(
                "gameId", gameId,
                "aircraftCode", request.aircraftCode(),
                "diceValue", request.diceValue()
        ));
    }

    /**
     * Lists valid landing targets after damage resolution.
     */
    public JsonNode listAvailableLandingBases(String gameId, String aircraftCode) {
        return toolExecutor.execute(SmartAirBaseTool.LIST_AVAILABLE_LANDING_BASES, Map.of(
                "gameId", gameId,
                "aircraftCode", aircraftCode
        ));
    }

    public LandingOptionsView getLandingOptionsView(String gameId, String aircraftCode) {
        return objectMapper.convertValue(listAvailableLandingBases(gameId, aircraftCode), LandingOptionsView.class);
    }

    /**
     * Lands an aircraft at the chosen base.
     */
    public JsonNode landAircraft(String gameId, LandAircraftRequest request) {
        return toolExecutor.execute(SmartAirBaseTool.LAND_AIRCRAFT, Map.of(
                "gameId", gameId,
                "aircraftCode", request.aircraftCode(),
                "baseCode", request.baseCode()
        ));
    }

    /**
     * Sends an aircraft to holding when no base can receive it.
     */
    public JsonNode sendAircraftToHolding(String gameId, String aircraftCode) {
        return toolExecutor.execute(SmartAirBaseTool.SEND_AIRCRAFT_TO_HOLDING, Map.of(
                "gameId", gameId,
                "aircraftCode", aircraftCode
        ));
    }

    /**
     * Finalizes the round and applies end-of-round effects.
     */
    public JsonNode completeRound(String gameId) {
        return toolExecutor.execute(SmartAirBaseTool.COMPLETE_ROUND, Map.of("gameId", gameId));
    }

    /**
     * Returns the state of one aircraft.
     */
    public JsonNode getAircraftState(String gameId, String aircraftCode) {
        return toolExecutor.execute(SmartAirBaseTool.GET_AIRCRAFT_STATE, Map.of(
                "gameId", gameId,
                "aircraftCode", aircraftCode
        ));
    }

    /**
     * Returns the state of one base.
     */
    public JsonNode getBaseState(String gameId, String baseCode) {
        return toolExecutor.execute(SmartAirBaseTool.GET_BASE_STATE, Map.of(
                "gameId", gameId,
                "baseCode", baseCode
        ));
    }
}
