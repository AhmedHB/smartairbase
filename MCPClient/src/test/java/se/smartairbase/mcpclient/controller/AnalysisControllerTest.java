package se.smartairbase.mcpclient.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import se.smartairbase.mcpclient.controller.dto.AnalysisFeedItemDTO;
import se.smartairbase.mcpclient.controller.dto.AnalysisFeedResponseDTO;
import se.smartairbase.mcpclient.service.AnalysisFeedService;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnalysisControllerTest {

    private AnalysisFeedService analysisFeedService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        analysisFeedService = mock(AnalysisFeedService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AnalysisController(analysisFeedService)).build();
    }

    @Test
    void getAnalysisFeedDelegatesToService() throws Exception {
        when(analysisFeedService.getFeed("13")).thenReturn(feedResponse());

        mockMvc.perform(get("/api/games/13/analysis-feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].role").value("Captain Erik Holm (Pilot)"));

        verify(analysisFeedService).getFeed("13");
    }

    @Test
    void generateAnalysisDelegatesToService() throws Exception {
        when(analysisFeedService.generateRoundAnalysis("13")).thenReturn(feedResponse());

        mockMvc.perform(post("/api/games/13/analysis/generate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastAnalyzedRound").value(4));

        verify(analysisFeedService).generateRoundAnalysis("13");
    }

    private AnalysisFeedResponseDTO feedResponse() {
        return new AnalysisFeedResponseDTO(
                List.of(new AnalysisFeedItemDTO(
                        "1",
                        13L,
                        4,
                        "ROUND_COMPLETE",
                        "Captain Erik Holm (Pilot)",
                        "LLM",
                        "Mission flown.",
                        null,
                        List.of("F1"),
                        List.of("BASE_A"),
                        "2026-03-11T12:00:00Z"
                )),
                false,
                4
        );
    }
}
