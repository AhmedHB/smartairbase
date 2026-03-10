package se.smartairbase.mcpclient.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import se.smartairbase.mcpclient.controller.dto.AutoPlayResponse;
import se.smartairbase.mcpclient.domain.GameRulesReference;
import se.smartairbase.mcpclient.service.AutoPlayService;
import se.smartairbase.mcpclient.service.GameRulesReferenceService;
import se.smartairbase.mcpclient.service.SmartAirBaseMcpClient;
import se.smartairbase.mcpclient.controller.dto.CreateGameRequest;
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
        JsonNode response = objectMapper.readTree("{\"gameId\":11,\"name\":\"smartairbase-v7\"}");
        CreateGameRequest request = new CreateGameRequest("smartairbase", "7");
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
                                {"scenarioName":"smartairbase","version":"v7"}
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
        JsonNode response = objectMapper.readTree("{\"roundNumber\":1}");
        when(mcpClient.startRound("13")).thenReturn(response);

        mockMvc.perform(post("/api/games/13/rounds/start"))
                .andExpect(status().isOk());

        verify(mcpClient).startRound("13");
    }

    @Test
    void nextRoundDelegatesToAutoplayService() throws Exception {
        AutoPlayResponse response = new AutoPlayResponse(
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
}
