package se.smartairbase.mcpclient.controller;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.smartairbase.mcpclient.controller.dto.AutoPlayResponseDTO;
import se.smartairbase.mcpclient.domain.GameRulesReference;
import se.smartairbase.mcpclient.service.AutoPlayService;
import se.smartairbase.mcpclient.service.GameRulesReferenceService;
import se.smartairbase.mcpclient.service.SmartAirBaseMcpClient;
import se.smartairbase.mcpclient.controller.dto.AssignMissionRequestDTO;
import se.smartairbase.mcpclient.controller.dto.ActionResultDTO;
import se.smartairbase.mcpclient.controller.dto.AircraftStateDTO;
import se.smartairbase.mcpclient.controller.dto.CreateGameRequestDTO;
import se.smartairbase.mcpclient.controller.dto.DiceRollRequestDTO;
import se.smartairbase.mcpclient.controller.dto.BaseStateDTO;
import se.smartairbase.mcpclient.controller.dto.GameStateDTO;
import se.smartairbase.mcpclient.controller.dto.GameSummaryDTO;
import se.smartairbase.mcpclient.controller.dto.LandAircraftRequestDTO;
import se.smartairbase.mcpclient.controller.dto.LandingOptionsDTO;
import se.smartairbase.mcpclient.controller.dto.RoundExecutionResultDTO;

/**
 * HTTP facade for the browser-based Smart Air Base client.
 *
 * <p>The controller is intentionally thin. It forwards player actions to the MCP
 * server through {@link SmartAirBaseMcpClient} and exposes a small read endpoint
 * for the local rules reference.</p>
 */
@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class GameController {

    private final SmartAirBaseMcpClient mcpClient;
    private final AutoPlayService autoPlayService;
    private final GameRulesReferenceService gameRulesReferenceService;

    public GameController(SmartAirBaseMcpClient mcpClient,
                          AutoPlayService autoPlayService,
                          GameRulesReferenceService gameRulesReferenceService) {
        this.mcpClient = mcpClient;
        this.autoPlayService = autoPlayService;
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
    public GameSummaryDTO createGame(@Valid @RequestBody CreateGameRequestDTO request) {
        return mcpClient.createGame(request);
    }

    /**
     * Returns the current consolidated game state.
     */
    @GetMapping("/games/{gameId}")
    public GameStateDTO getGameState(@PathVariable String gameId) {
        return mcpClient.getGameState(gameId);
    }

    /**
     * Starts a new round for the selected game.
     */
    @PostMapping("/games/{gameId}/rounds/start")
    public RoundExecutionResultDTO startRound(@PathVariable String gameId) {
        return mcpClient.startRound(gameId);
    }

    /**
     * Starts the next round and lets the client assign missions automatically.
     */
    @PostMapping("/games/{gameId}/rounds/next")
    public AutoPlayResponseDTO startNextRound(@PathVariable String gameId) {
        return autoPlayService.startNextRound(gameId);
    }

    /**
     * Starts the next round and performs automatic mission assignment without resolving missions.
     */
    @PostMapping("/games/{gameId}/rounds/plan")
    public AutoPlayResponseDTO planNextRound(@PathVariable String gameId) {
        return autoPlayService.planNextRound(gameId);
    }

    /**
     * Assigns one mission to one aircraft.
     */
    @PostMapping("/games/{gameId}/missions/assign")
    public ActionResultDTO assignMission(@PathVariable String gameId, @Valid @RequestBody AssignMissionRequestDTO request) {
        return mcpClient.assignMission(gameId, request);
    }

    /**
     * Resolves all currently assigned missions in the active round.
     */
    @PostMapping("/games/{gameId}/missions/resolve")
    public RoundExecutionResultDTO resolveMissions(@PathVariable String gameId) {
        return mcpClient.resolveMissions(gameId);
    }

    /**
     * Resolves already planned missions and returns an autoplay-style response for the UI.
     */
    @PostMapping("/games/{gameId}/missions/resolve-auto")
    public AutoPlayResponseDTO resolvePlannedMissions(@PathVariable String gameId) {
        return autoPlayService.resolvePlannedMissions(gameId);
    }

    /**
     * Records the dice outcome for one aircraft.
     */
    @PostMapping("/games/{gameId}/dice-rolls")
    public ActionResultDTO recordDiceRoll(@PathVariable String gameId, @Valid @RequestBody DiceRollRequestDTO request) {
        return mcpClient.recordDiceRoll(gameId, request);
    }

    /**
     * Records one dice roll and lets the client resolve landings and round completion automatically.
     */
    @PostMapping("/games/{gameId}/dice-rolls/auto")
    public AutoPlayResponseDTO resolveDiceRollAutomatically(@PathVariable String gameId,
                                                            @Valid @RequestBody DiceRollRequestDTO request) {
        return autoPlayService.resolveDiceRoll(gameId, request);
    }

    /**
     * Lists landing bases that are currently valid for one aircraft.
     */
    @GetMapping("/games/{gameId}/landing-bases")
    public LandingOptionsDTO listAvailableLandingBases(@PathVariable String gameId, @RequestParam String aircraftCode) {
        return mcpClient.listAvailableLandingBases(gameId, aircraftCode);
    }

    /**
     * Lands an aircraft at a selected base.
     */
    @PostMapping("/games/{gameId}/landings")
    public ActionResultDTO landAircraft(@PathVariable String gameId, @Valid @RequestBody LandAircraftRequestDTO request) {
        return mcpClient.landAircraft(gameId, request);
    }

    /**
     * Sends an aircraft to holding if landing is not possible.
     */
    @PostMapping("/games/{gameId}/holding")
    public ActionResultDTO sendAircraftToHolding(@PathVariable String gameId, @RequestParam String aircraftCode) {
        return mcpClient.sendAircraftToHolding(gameId, aircraftCode);
    }

    /**
     * Completes the active round after all pending decisions are resolved.
     */
    @PostMapping("/games/{gameId}/rounds/complete")
    public RoundExecutionResultDTO completeRound(@PathVariable String gameId) {
        return mcpClient.completeRound(gameId);
    }

    /**
     * Returns the current runtime state for one aircraft.
     */
    @GetMapping("/games/{gameId}/aircraft/{aircraftCode}")
    public AircraftStateDTO getAircraftState(@PathVariable String gameId, @PathVariable String aircraftCode) {
        return mcpClient.getAircraftState(gameId, aircraftCode);
    }

    /**
     * Returns the current runtime state for one base.
     */
    @GetMapping("/games/{gameId}/bases/{baseCode}")
    public BaseStateDTO getBaseState(@PathVariable String gameId, @PathVariable String baseCode) {
        return mcpClient.getBaseState(gameId, baseCode);
    }
}
