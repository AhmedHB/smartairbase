package se.smartairbase.mcpclient.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import se.smartairbase.mcpclient.controller.dto.GameSummaryDTO;
import se.smartairbase.mcpclient.controller.dto.RoundExecutionResultDTO;
import se.smartairbase.mcpclient.controller.dto.AutoPlayResponseDTO;
import se.smartairbase.mcpclient.controller.dto.ActionResultDTO;
import se.smartairbase.mcpclient.domain.GameRulesReference;
import se.smartairbase.mcpclient.service.AutoPlayService;
import se.smartairbase.mcpclient.service.GameRulesReferenceService;
import se.smartairbase.mcpclient.service.SmartAirBaseMcpClient;
import se.smartairbase.mcpclient.controller.dto.CreateGameRequestDTO;
import se.smartairbase.mcpclient.service.TestStateFactory;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
                .setValidator(validator)
                .build();
    }

    @Test
    void createGameDelegatesToClient() throws Exception {
        GameSummaryDTO response = new GameSummaryDTO(11L, "smartairbase-v7", "smartairbase", "7", "ACTIVE", 0, null, false, true, false);
        CreateGameRequestDTO request = new CreateGameRequestDTO("smartairbase", "7", null, null);
        when(mcpClient.createGame(eq(request))).thenReturn(response);

        mockMvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(mcpClient).createGame(eq(request));
    }

    @Test
    void createGameRejectsNonNumericVersion() throws Exception {
        mockMvc.perform(post("/api/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scenarioName":"smartairbase","version":"seven"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recordDiceRollRejectsOutOfRangeValue() throws Exception {
        mockMvc.perform(post("/api/games/5/dice-rolls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"aircraftCode":"F1","diceValue":9}
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
