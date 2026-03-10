package se.smartairbase.mcpclient.controller;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.smartairbase.mcpclient.domain.GameRulesReference;
import se.smartairbase.mcpclient.service.GameRulesReferenceService;
import se.smartairbase.mcpclient.service.SmartAirBaseMcpClient;
import se.smartairbase.mcpclient.controller.dto.AssignMissionRequest;
import se.smartairbase.mcpclient.controller.dto.CreateGameRequest;
import se.smartairbase.mcpclient.controller.dto.DiceRollRequest;
import se.smartairbase.mcpclient.controller.dto.LandAircraftRequest;

@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
/**
 * HTTP facade for the browser-based Smart Air Base client.
 *
 * <p>The controller is intentionally thin. It forwards player actions to the MCP
 * server through {@link SmartAirBaseMcpClient} and exposes a small read endpoint
 * for the local rules reference.</p>
 */
public class GameController {

    private final SmartAirBaseMcpClient mcpClient;
    private final GameRulesReferenceService gameRulesReferenceService;

    public GameController(SmartAirBaseMcpClient mcpClient, GameRulesReferenceService gameRulesReferenceService) {
        this.mcpClient = mcpClient;
        this.gameRulesReferenceService = gameRulesReferenceService;
    }

    /**
     * Returns the locally documented rule summary shown in the UI.
     */
    @GetMapping("/reference/rules")
    public GameRulesReference getRules() {
        return gameRulesReferenceService.getRules();
    }

    /**
     * Creates a new game from the requested scenario/version pair.
     */
    @PostMapping("/games")
    public JsonNode createGame(@Valid @RequestBody CreateGameRequest request) {
        return mcpClient.createGame(request);
    }

    /**
     * Returns the current consolidated game state.
     */
    @GetMapping("/games/{gameId}")
    public JsonNode getGameState(@PathVariable String gameId) {
        return mcpClient.getGameState(gameId);
    }

    /**
     * Starts a new round for the selected game.
     */
    @PostMapping("/games/{gameId}/rounds/start")
    public JsonNode startRound(@PathVariable String gameId) {
        return mcpClient.startRound(gameId);
    }

    /**
     * Assigns one mission to one aircraft.
     */
    @PostMapping("/games/{gameId}/missions/assign")
    public JsonNode assignMission(@PathVariable String gameId, @Valid @RequestBody AssignMissionRequest request) {
        return mcpClient.assignMission(gameId, request);
    }

    /**
     * Resolves all currently assigned missions in the active round.
     */
    @PostMapping("/games/{gameId}/missions/resolve")
    public JsonNode resolveMissions(@PathVariable String gameId) {
        return mcpClient.resolveMissions(gameId);
    }

    /**
     * Records the dice outcome for one aircraft.
     */
    @PostMapping("/games/{gameId}/dice-rolls")
    public JsonNode recordDiceRoll(@PathVariable String gameId, @Valid @RequestBody DiceRollRequest request) {
        return mcpClient.recordDiceRoll(gameId, request);
    }

    /**
     * Lists landing bases that are currently valid for one aircraft.
     */
    @GetMapping("/games/{gameId}/landing-bases")
    public JsonNode listAvailableLandingBases(@PathVariable String gameId, @RequestParam String aircraftCode) {
        return mcpClient.listAvailableLandingBases(gameId, aircraftCode);
    }

    /**
     * Lands an aircraft at a selected base.
     */
    @PostMapping("/games/{gameId}/landings")
    public JsonNode landAircraft(@PathVariable String gameId, @Valid @RequestBody LandAircraftRequest request) {
        return mcpClient.landAircraft(gameId, request);
    }

    /**
     * Sends an aircraft to holding if landing is not possible.
     */
    @PostMapping("/games/{gameId}/holding")
    public JsonNode sendAircraftToHolding(@PathVariable String gameId, @RequestParam String aircraftCode) {
        return mcpClient.sendAircraftToHolding(gameId, aircraftCode);
    }

    /**
     * Completes the active round after all pending decisions are resolved.
     */
    @PostMapping("/games/{gameId}/rounds/complete")
    public JsonNode completeRound(@PathVariable String gameId) {
        return mcpClient.completeRound(gameId);
    }

    /**
     * Returns the current runtime state for one aircraft.
     */
    @GetMapping("/games/{gameId}/aircraft/{aircraftCode}")
    public JsonNode getAircraftState(@PathVariable String gameId, @PathVariable String aircraftCode) {
        return mcpClient.getAircraftState(gameId, aircraftCode);
    }

    /**
     * Returns the current runtime state for one base.
     */
    @GetMapping("/games/{gameId}/bases/{baseCode}")
    public JsonNode getBaseState(@PathVariable String gameId, @PathVariable String baseCode) {
        return mcpClient.getBaseState(gameId, baseCode);
    }
}
