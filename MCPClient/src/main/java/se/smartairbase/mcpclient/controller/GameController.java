package se.smartairbase.mcpclient.controller;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
import se.smartairbase.mcpclient.controller.dto.CreateScenarioGameRequestDTO;
import se.smartairbase.mcpclient.controller.dto.CreateSimulationBatchRequestDTO;
import se.smartairbase.mcpclient.controller.dto.DiceRollRequestDTO;
import se.smartairbase.mcpclient.controller.dto.DuplicateScenarioRequestDTO;
import se.smartairbase.mcpclient.controller.dto.BaseStateDTO;
import se.smartairbase.mcpclient.controller.dto.GameStateDTO;
import se.smartairbase.mcpclient.controller.dto.GameSummaryDTO;
import se.smartairbase.mcpclient.controller.dto.GameAnalyticsSnapshotDTO;
import se.smartairbase.mcpclient.controller.dto.LandAircraftRequestDTO;
import se.smartairbase.mcpclient.controller.dto.LandingOptionsDTO;
import se.smartairbase.mcpclient.controller.dto.RoundExecutionResultDTO;
import se.smartairbase.mcpclient.controller.dto.ScenarioDefinitionDTO;
import se.smartairbase.mcpclient.controller.dto.ScenarioSummaryDTO;
import se.smartairbase.mcpclient.controller.dto.SimulationBatchDTO;
import se.smartairbase.mcpclient.controller.dto.UpdateScenarioRequestDTO;

import java.util.List;

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
     *
     * <p>An optional game name may be supplied by the frontend. If it is omitted,
     * the server assigns a generated default name. Custom names must be unique.</p>
     */
    @PostMapping("/games")
    public GameSummaryDTO createGame(@Valid @RequestBody CreateGameRequestDTO request) {
        return mcpClient.createGame(request);
    }

    /**
     * Creates one saved simulation batch that runs multiple games in the background.
     */
    @PostMapping("/simulations")
    public SimulationBatchDTO createSimulationBatch(@Valid @RequestBody CreateSimulationBatchRequestDTO request) {
        return mcpClient.createSimulationBatch(request);
    }

    /**
     * Returns progress for one background simulation batch.
     */
    @GetMapping("/simulations/{simulationBatchId}")
    public SimulationBatchDTO getSimulationBatch(@PathVariable String simulationBatchId) {
        return mcpClient.getSimulationBatch(simulationBatchId);
    }

    /**
     * Returns finished-game analytics rows, newest first, with optional filters.
     */
    @GetMapping("/analytics/games")
    public List<GameAnalyticsSnapshotDTO> listGameAnalyticsSnapshots(@RequestParam(required = false) String scenarioName,
                                                                     @RequestParam(required = false) String createdDate,
                                                                     @RequestParam(required = false) Integer aircraftCount,
                                                                     @RequestParam(required = false) Integer m1Count,
                                                                     @RequestParam(required = false) Integer m2Count,
                                                                     @RequestParam(required = false) Integer m3Count) {
        return mcpClient.listGameAnalyticsSnapshots(scenarioName, createdDate, aircraftCount, m1Count, m2Count, m3Count);
    }

    /**
     * Lists available scenarios for scenario browsing and duplication.
     */
    @GetMapping("/scenarios")
    public List<ScenarioSummaryDTO> listScenarios() {
        return mcpClient.listScenarios();
    }

    /**
     * Returns one scenario definition with its configuration details.
     */
    @GetMapping("/scenarios/{scenarioId}")
    public ScenarioDefinitionDTO getScenario(@PathVariable String scenarioId) {
        return mcpClient.getScenario(scenarioId);
    }

    /**
     * Duplicates one scenario into a user-editable copy.
     */
    @PostMapping("/scenarios/{scenarioId}/duplicate")
    public ScenarioDefinitionDTO duplicateScenario(@PathVariable String scenarioId,
                                                   @Valid @RequestBody DuplicateScenarioRequestDTO request) {
        return mcpClient.duplicateScenario(scenarioId, request);
    }

    /**
     * Updates one editable user-created scenario.
     */
    @PutMapping("/scenarios/{scenarioId}")
    public ScenarioDefinitionDTO updateScenario(@PathVariable String scenarioId,
                                                @Valid @RequestBody UpdateScenarioRequestDTO request) {
        return mcpClient.updateScenario(scenarioId, request);
    }

    /**
     * Deletes one user-created scenario.
     */
    @DeleteMapping("/scenarios/{scenarioId}")
    public ActionResultDTO deleteScenario(@PathVariable String scenarioId) {
        return mcpClient.deleteScenario(scenarioId);
    }

    /**
     * Creates a game directly from one selected scenario.
     */
    @PostMapping("/scenarios/{scenarioId}/create-game")
    public GameSummaryDTO createGameFromScenario(@PathVariable String scenarioId,
                                                 @RequestBody(required = false) CreateScenarioGameRequestDTO request) {
        return mcpClient.createGameFromScenario(scenarioId, request == null ? new CreateScenarioGameRequestDTO(null) : request);
    }

    /**
     * Returns the current consolidated game state.
     */
    @GetMapping("/games/{gameId}")
    public GameStateDTO getGameState(@PathVariable String gameId) {
        return mcpClient.getGameState(gameId);
    }

    /**
     * Aborts one game and makes it unavailable for further play.
     */
    @PostMapping("/games/{gameId}/abort")
    public ActionResultDTO abortGame(@PathVariable String gameId) {
        return mcpClient.abortGame(gameId);
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
