package se.smartairbase.mcpclient.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import se.smartairbase.mcpclient.controller.dto.GameSummaryDTO;
import se.smartairbase.mcpclient.controller.dto.GameAnalyticsSnapshotDTO;
import se.smartairbase.mcpclient.controller.dto.RoundExecutionResultDTO;
import se.smartairbase.mcpclient.controller.dto.AutoPlayResponseDTO;
import se.smartairbase.mcpclient.controller.dto.ActionResultDTO;
import se.smartairbase.mcpclient.controller.dto.CreateScenarioGameRequestDTO;
import se.smartairbase.mcpclient.controller.dto.DuplicateScenarioRequestDTO;
import se.smartairbase.mcpclient.controller.dto.ScenarioDefinitionDTO;
import se.smartairbase.mcpclient.controller.dto.ScenarioSummaryDTO;
import se.smartairbase.mcpclient.controller.dto.ScenarioAircraftDTO;
import se.smartairbase.mcpclient.controller.dto.ScenarioBaseDTO;
import se.smartairbase.mcpclient.controller.dto.ScenarioMissionDTO;
import se.smartairbase.mcpclient.controller.dto.UpdateScenarioRequestDTO;
import se.smartairbase.mcpclient.domain.GameRulesReference;
import se.smartairbase.mcpclient.service.AutoPlayService;
import se.smartairbase.mcpclient.service.GameRulesReferenceService;
import se.smartairbase.mcpclient.service.SmartAirBaseMcpClient;
import se.smartairbase.mcpclient.controller.dto.CreateGameRequestDTO;
import se.smartairbase.mcpclient.controller.dto.CreateSimulationBatchRequestDTO;
import se.smartairbase.mcpclient.service.TestStateFactory;
import se.smartairbase.mcpclient.controller.dto.SimulationBatchDTO;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GameControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SmartAirBaseMcpClient mcpClient;
    private AutoPlayService autoPlayService;
    private GameRulesReferenceService gameRulesReferenceService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mcpClient = mock(SmartAirBaseMcpClient.class);
        autoPlayService = mock(AutoPlayService.class);
        gameRulesReferenceService = mock(GameRulesReferenceService.class);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new GameController(mcpClient, autoPlayService, gameRulesReferenceService))
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createGameDelegatesToClient() throws Exception {
        GameSummaryDTO response = new GameSummaryDTO(11L, "smartairbase-v7", "smartairbase", "7", "ACTIVE", 0, null, false, true, false, 1000);
        CreateGameRequestDTO request = new CreateGameRequestDTO("smartairbase", "7", "GAME_001", null, null, 1000);
        when(mcpClient.createGame(eq(request))).thenReturn(response);

        mockMvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(mcpClient).createGame(eq(request));
    }

    @Test
    void createSimulationBatchDelegatesToClient() throws Exception {
        CreateSimulationBatchRequestDTO request = new CreateSimulationBatchRequestDTO(
                "SIM_BATCH",
                "SCN_STANDARD",
                3,
                java.util.Map.of("M1", 1, "M2", 1, "M3", 1),
                "RANDOM",
                10,
                1000
        );
        when(mcpClient.createSimulationBatch(request)).thenReturn(new SimulationBatchDTO(
                41L, "SIM_BATCH", "SCN_STANDARD", 3, 1, 1, 1, "RANDOM", 1000, 10, 0, 0, 0, 0, "PENDING", null
        ));

        mockMvc.perform(post("/api/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulationBatchId").value(41))
                .andExpect(jsonPath("$.name").value("SIM_BATCH"));

        verify(mcpClient).createSimulationBatch(request);
    }

    @Test
    void createGameRejectsNonPositiveMaxRounds() throws Exception {
        mockMvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scenarioName":"SCN_STANDARD","version":"7","maxRounds":0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createSimulationBatchRejectsNonPositiveMaxRounds() throws Exception {
        mockMvc.perform(post("/api/simulations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"batchName":"SIM_BATCH","scenarioName":"SCN_STANDARD","aircraftCount":3,"runCount":10,"maxRounds":0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSimulationBatchDelegatesToClient() throws Exception {
        when(mcpClient.getSimulationBatch("41")).thenReturn(new SimulationBatchDTO(
                41L, "SIM_BATCH", "SCN_STANDARD", 3, 1, 1, 1, "RANDOM", 1000, 10, 6, 0, 4, 2, "RUNNING", "SIM_BATCH_007"
        ));

        mockMvc.perform(get("/api/simulations/41"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.currentGameName").value("SIM_BATCH_007"));

        verify(mcpClient).getSimulationBatch("41");
    }

    @Test
    void listGameAnalyticsSnapshotsDelegatesToClient() throws Exception {
        when(mcpClient.listGameAnalyticsSnapshots("SCN_STANDARD", "2026-03-13", 3, 1, 1, 1))
                .thenReturn(java.util.List.of(
                        new GameAnalyticsSnapshotDTO(7L, 11L, "GAME_001", "SCN_STANDARD", "WON", true, 2, "AUTO_RANDOM", 3, 3, 0, 3, 3, 1, 1, 1, "2026-03-13T10:00:00")
                ));

        mockMvc.perform(get("/api/analytics/games")
                        .param("scenarioName", "SCN_STANDARD")
                        .param("createdDate", "2026-03-13")
                        .param("aircraftCount", "3")
                        .param("m1Count", "1")
                        .param("m2Count", "1")
                        .param("m3Count", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].gameName").value("GAME_001"));

        verify(mcpClient).listGameAnalyticsSnapshots("SCN_STANDARD", "2026-03-13", 3, 1, 1, 1);
    }

    @Test
    void listScenariosDelegatesToClient() throws Exception {
        when(mcpClient.listScenarios()).thenReturn(java.util.List.of(
                new ScenarioSummaryDTO(1L, "Standard Scenario", "V7", "SYSTEM", false, false, true)
        ));

        mockMvc.perform(get("/api/scenarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Standard Scenario"))
                .andExpect(jsonPath("$[0].sourceType").value("SYSTEM"));

        verify(mcpClient).listScenarios();
    }

    @Test
    void getScenarioDelegatesToClient() throws Exception {
        when(mcpClient.getScenario("1")).thenReturn(new ScenarioDefinitionDTO(
                1L, "Standard Scenario", "V7", "desc", "SYSTEM", false, false, true,
                java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of()
        ));

        mockMvc.perform(get("/api/scenarios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Standard Scenario"))
                .andExpect(jsonPath("$.version").value("V7"));

        verify(mcpClient).getScenario("1");
    }

    @Test
    void duplicateScenarioDelegatesToClient() throws Exception {
        DuplicateScenarioRequestDTO request = new DuplicateScenarioRequestDTO("STANDARD_SCENARIO_COPY");
        when(mcpClient.duplicateScenario("1", request)).thenReturn(new ScenarioDefinitionDTO(
                2L, "STANDARD_SCENARIO_COPY", "V7", "desc", "USER", true, true, false,
                java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of()
        ));

        mockMvc.perform(post("/api/scenarios/1/duplicate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("STANDARD_SCENARIO_COPY"))
                .andExpect(jsonPath("$.sourceType").value("USER"));

        verify(mcpClient).duplicateScenario("1", request);
    }

    @Test
    void duplicateScenarioRejectsInvalidNameCharacters() throws Exception {
        mockMvc.perform(post("/api/scenarios/1/duplicate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"invalid name!"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createGameFromScenarioDelegatesToClient() throws Exception {
        CreateScenarioGameRequestDTO request = new CreateScenarioGameRequestDTO("Scenario test", 4, java.util.Map.of("M1", 2), 250);
        when(mcpClient.createGameFromScenario("1", request))
                .thenReturn(new GameSummaryDTO(21L, "Scenario test", "SCN_STANDARD", "V7", "ACTIVE", 0, null, false, true, false, 1000));

        mockMvc.perform(post("/api/scenarios/1/create-game")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(21));

        verify(mcpClient).createGameFromScenario("1", request);
    }

    @Test
    void updateScenarioDelegatesToClient() throws Exception {
        UpdateScenarioRequestDTO request = new UpdateScenarioRequestDTO(
                "Updated description",
                java.util.List.of(new ScenarioBaseDTO("A", "Base A", "MAIN", 5, 3, 100, 100, 0, 0, 0, 0, java.util.List.of())),
                java.util.List.of(new ScenarioAircraftDTO("F1", "JAS39", "A", 90, 4, 18)),
                java.util.List.of(new ScenarioMissionDTO("M1", "M1", "Recon", 1, 1, 25, 1, 5))
        );
        when(mcpClient.updateScenario("2", request)).thenReturn(new ScenarioDefinitionDTO(
                2L, "WINTER_OPS", "V7", "desc", "USER", true, true, false,
                request.bases(), request.aircraft(), request.missions(), java.util.List.of()
        ));

        mockMvc.perform(put("/api/scenarios/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aircraft[0].fuelStart").value(90))
                .andExpect(jsonPath("$.missions[0].fuelCost").value(25));

        verify(mcpClient).updateScenario("2", request);
    }

    @Test
    void deleteScenarioDelegatesToClient() throws Exception {
        when(mcpClient.deleteScenario("2")).thenReturn(new ActionResultDTO(true, "Scenario deleted"));

        mockMvc.perform(delete("/api/scenarios/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Scenario deleted"));

        verify(mcpClient).deleteScenario("2");
    }

    @Test
    void createGameRejectsNonNumericVersion() throws Exception {
        mockMvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scenarioName":"smartairbase","version":"seven","gameName":"GAME_001"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createGameRejectsBlankGameNameWhenProvided() throws Exception {
        mockMvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scenarioName":"smartairbase","version":"7","gameName":"   "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createGameReturnsBadRequestWhenNameAlreadyExists() throws Exception {
        CreateGameRequestDTO request = new CreateGameRequestDTO("smartairbase", "7", "GAME_001", null, null, 1000);
        when(mcpClient.createGame(eq(request))).thenThrow(new IllegalArgumentException("The game name \"GAME_001\" is already in use. Choose a different name."));

        mockMvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The game name \"GAME_001\" is already in use. Choose a different name."));
    }

    @Test
    void recordDiceRollRejectsOutOfRangeValue() throws Exception {
        mockMvc.perform(post("/api/games/5/dice-rolls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"aircraftCode":"F1","diceValue":9,"diceSelectionMode":"MANUAL_DIRECT_SELECTION"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recordDiceRollRejectsUnknownSelectionMode() throws Exception {
        mockMvc.perform(post("/api/games/5/dice-rolls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"aircraftCode":"F1","diceValue":4,"diceSelectionMode":"SOMETHING_ELSE"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRulesReturnsReferenceData() throws Exception {
        GameRulesReference rules = new GameRulesReference(
                "7",
                java.util.List.of("Complete all missions"),
                null,
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                null,
                java.util.List.of("1. Planning")
        );
        when(gameRulesReferenceService.getRules()).thenReturn(rules);

        mockMvc.perform(get("/api/reference/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value("7"))
                .andExpect(jsonPath("$.objectives[0]").value("Complete all missions"));
    }

    @Test
    void startRoundDelegatesUsingPathVariable() throws Exception {
        RoundExecutionResultDTO response = new RoundExecutionResultDTO(13L, 1, "PLANNING", "ACTIVE", true,
                java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of("Round started"));
        when(mcpClient.startRound("13")).thenReturn(response);

        mockMvc.perform(post("/api/games/13/rounds/start"))
                .andExpect(status().isOk());

        verify(mcpClient).startRound("13");
    }

    @Test
    void abortGameDelegatesUsingPathVariable() throws Exception {
        ActionResultDTO response = new ActionResultDTO(true, "Game aborted");
        when(mcpClient.abortGame("13")).thenReturn(response);

        mockMvc.perform(post("/api/games/13/abort"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Game aborted"));

        verify(mcpClient).abortGame("13");
    }

    @Test
    void nextRoundDelegatesToAutoplayService() throws Exception {
        AutoPlayResponseDTO response = new AutoPlayResponseDTO(
                TestStateFactory.state(
                        TestStateFactory.summary(1, "DICE_ROLL", true, false, false, "ACTIVE"),
                        java.util.List.of(TestStateFactory.aircraft("F1", "AWAITING_DICE_ROLL", null, 80, 6, 16, "NONE")),
                        java.util.List.of(TestStateFactory.mission("M1", "COMPLETED"))
                ),
                "ROLL_DICE",
                false,
                false,
                java.util.List.of("F1"),
                java.util.List.of("F1 -> M1"),
                java.util.List.of(),
                java.util.List.of("Round started")
        );
        when(autoPlayService.startNextRound("13")).thenReturn(response);

        mockMvc.perform(post("/api/games/13/rounds/next"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextAction").value("ROLL_DICE"))
                .andExpect(jsonPath("$.pendingDiceAircraft[0]").value("F1"));

        verify(autoPlayService).startNextRound("13");
    }

    @Test
    void planNextRoundDelegatesToAutoplayService() throws Exception {
        AutoPlayResponseDTO response = new AutoPlayResponseDTO(
                TestStateFactory.state(
                        TestStateFactory.summary(1, "PLANNING", true, false, false, "ACTIVE"),
                        java.util.List.of(TestStateFactory.aircraft("F1", "READY", "A", 100, 6, 20, "NONE")),
                        java.util.List.of(TestStateFactory.mission("M1", "AVAILABLE"))
                ),
                "RESOLVE_MISSIONS",
                false,
                false,
                java.util.List.of(),
                java.util.List.of("F1 -> M1"),
                java.util.List.of(),
                java.util.List.of("Round started")
        );
        when(autoPlayService.planNextRound("13")).thenReturn(response);

        mockMvc.perform(post("/api/games/13/rounds/plan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextAction").value("RESOLVE_MISSIONS"));

        verify(autoPlayService).planNextRound("13");
    }

    @Test
    void resolvePlannedMissionsDelegatesToAutoplayService() throws Exception {
        AutoPlayResponseDTO response = new AutoPlayResponseDTO(
                TestStateFactory.state(
                        TestStateFactory.summary(1, "DICE_ROLL", true, false, false, "ACTIVE"),
                        java.util.List.of(TestStateFactory.aircraft("F1", "AWAITING_DICE_ROLL", null, 80, 6, 16, "NONE")),
                        java.util.List.of(TestStateFactory.mission("M1", "COMPLETED"))
                ),
                "ROLL_DICE",
                false,
                false,
                java.util.List.of("F1"),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of("Mission resolution completed")
        );
        when(autoPlayService.resolvePlannedMissions("13")).thenReturn(response);

        mockMvc.perform(post("/api/games/13/missions/resolve-auto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingDiceAircraft[0]").value("F1"));

        verify(autoPlayService).resolvePlannedMissions("13");
    }
}
