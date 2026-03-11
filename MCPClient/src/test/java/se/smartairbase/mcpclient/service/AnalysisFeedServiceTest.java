package se.smartairbase.mcpclient.service;

import org.junit.jupiter.api.Test;
import se.smartairbase.mcpclient.controller.dto.AnalysisFeedItemDTO;
import se.smartairbase.mcpclient.controller.dto.AnalysisFeedResponseDTO;
import se.smartairbase.mcpclient.controller.dto.GameStateDTO;
import se.smartairbase.mcpclient.domain.AnalysisRole;
import se.smartairbase.mcpclient.service.analysis.AnalysisFactService;
import se.smartairbase.mcpclient.service.analysis.AnalysisNarration;
import se.smartairbase.mcpclient.service.analysis.AnalysisNarrationService;
import se.smartairbase.mcpclient.service.analysis.AnalysisRoundFacts;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisFeedServiceTest {

    @Test
    void getFeedReturnsPersistedFeedFromServer() {
        SmartAirBaseMcpClient mcpClient = mock(SmartAirBaseMcpClient.class);
        AnalysisFactService factService = mock(AnalysisFactService.class);
        AnalysisNarrationService narrationService = mock(AnalysisNarrationService.class);
        AnalysisFeedResponseDTO response = responseWithRound(3);
        when(mcpClient.listAnalysisFeed("9")).thenReturn(response);

        AnalysisFeedService service = new AnalysisFeedService(mcpClient, factService, narrationService);

        AnalysisFeedResponseDTO result = service.getFeed("9");

        assertThat(result).isSameAs(response);
        verify(mcpClient).listAnalysisFeed("9");
    }

    @Test
    void generateRoundAnalysisPersistsNewItemsWhenRoundNotAlreadyAnalyzed() {
        SmartAirBaseMcpClient mcpClient = mock(SmartAirBaseMcpClient.class);
        AnalysisFactService factService = mock(AnalysisFactService.class);
        AnalysisNarrationService narrationService = mock(AnalysisNarrationService.class);
        GameStateDTO currentState = TestStateFactory.state(
                TestStateFactory.summary(4, "ROUND_COMPLETE", false, true, false, "ACTIVE"),
                List.of(TestStateFactory.aircraft("F1", "READY", "BASE_A", 100, 6, 20, "NONE")),
                List.of(TestStateFactory.mission("M1", "COMPLETED"))
        );
        AnalysisRoundFacts facts = new AnalysisRoundFacts(
                1L, 4, "ROUND_COMPLETE", "ACTIVE",
                1, 1, 1, 0,
                List.of("F1"), List.of(), List.of(), List.of(), List.of(),
                List.of("F1"), List.of("F1"), List.of("BASE_A"), List.of("F1"), List.of("BASE_A")
        );
        AnalysisFactService.Snapshot snapshot = new AnalysisFactService.Snapshot(
                4, "ROUND_COMPLETE", "ACTIVE",
                java.util.Map.of("F1", TestStateFactory.aircraft("F1", "READY", "BASE_A", 100, 6, 20, "NONE")),
                java.util.Map.of("BASE_A", TestStateFactory.base("BASE_A", "Base A", 300, 20, 10, 1, 4, 0, 2)),
                1,
                1
        );
        when(mcpClient.getGameStateView("9")).thenReturn(currentState);
        when(mcpClient.listAnalysisFeed("9")).thenReturn(new AnalysisFeedResponseDTO(List.of(), false, null));
        when(factService.buildFacts(eq(currentState), any())).thenReturn(facts);
        when(factService.snapshot(currentState)).thenReturn(snapshot);
        when(narrationService.narrate(any(), eq(facts))).thenAnswer(invocation -> {
            AnalysisRole role = invocation.getArgument(0);
            return new AnalysisNarration("Rule-based", "Summary for " + role.name(), null);
        });
        when(mcpClient.appendAnalysisFeedItems(eq("9"), any())).thenReturn(responseWithRound(4));

        AnalysisFeedService service = new AnalysisFeedService(mcpClient, factService, narrationService);

        AnalysisFeedResponseDTO result = service.generateRoundAnalysis("9");

        assertThat(result.lastAnalyzedRound()).isEqualTo(4);
        verify(mcpClient).appendAnalysisFeedItems(eq("9"), any());
    }

    @Test
    void generateRoundAnalysisReturnsExistingFeedWhenRoundAlreadyAnalyzed() {
        SmartAirBaseMcpClient mcpClient = mock(SmartAirBaseMcpClient.class);
        AnalysisFactService factService = mock(AnalysisFactService.class);
        AnalysisNarrationService narrationService = mock(AnalysisNarrationService.class);
        GameStateDTO currentState = TestStateFactory.state(
                TestStateFactory.summary(4, "ROUND_COMPLETE", false, true, false, "ACTIVE"),
                List.of(TestStateFactory.aircraft("F1", "READY", "BASE_A", 100, 6, 20, "NONE")),
                List.of(TestStateFactory.mission("M1", "COMPLETED"))
        );
        AnalysisFeedResponseDTO existing = responseWithRound(4);
        when(mcpClient.getGameStateView("9")).thenReturn(currentState);
        when(mcpClient.listAnalysisFeed("9")).thenReturn(existing);

        AnalysisFeedService service = new AnalysisFeedService(mcpClient, factService, narrationService);

        AnalysisFeedResponseDTO result = service.generateRoundAnalysis("9");

        assertThat(result).isSameAs(existing);
        verify(mcpClient, never()).appendAnalysisFeedItems(eq("9"), any());
    }

    private AnalysisFeedResponseDTO responseWithRound(int round) {
        return new AnalysisFeedResponseDTO(
                List.of(new AnalysisFeedItemDTO(
                        "1",
                        1L,
                        round,
                        "ROUND_COMPLETE",
                        "Captain Erik Holm (Pilot)",
                        "Rule-based",
                        "Saved summary",
                        null,
                        List.of("F1"),
                        List.of("BASE_A"),
                        "2026-03-11T12:00:00Z"
                )),
                false,
                round
        );
    }
}
