package se.smartairbase.mcpserver.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import se.smartairbase.mcpserver.mcp.dto.AnalysisFeedItemDto;
import se.smartairbase.mcpserver.mcp.dto.AnalysisFeedResponseDto;
import se.smartairbase.mcpserver.mcp.dto.GameSummaryDto;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.liquibase.clear-checksums=true"
})
@Transactional
class AnalysisFeedPersistenceServiceTests {

    @Autowired
    private GameService gameService;

    @Autowired
    private AnalysisFeedPersistenceService analysisFeedPersistenceService;

    @Test
    void listFeedReturnsPersistedEntriesInChronologicalOrder() {
        Long gameId = createGame();

        analysisFeedPersistenceService.appendFeedItems(gameId, List.of(
                item(gameId, 1, "Captain Erik Holm (Pilot)", "Rule-based", "Pilot summary"),
                item(gameId, 1, "Sara Lind (Ground Crew Chief)", "LLM", "Ground summary")
        ));

        AnalysisFeedResponseDto response = analysisFeedPersistenceService.listFeed(gameId);

        assertThat(response.lastAnalyzedRound()).isEqualTo(1);
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).role()).isEqualTo("Captain Erik Holm (Pilot)");
        assertThat(response.items().get(0).source()).isEqualTo("Rule-based");
        assertThat(response.items().get(1).role()).isEqualTo("Sara Lind (Ground Crew Chief)");
        assertThat(response.items().get(1).source()).isEqualTo("LLM");
    }

    @Test
    void appendFeedItemsSkipsDuplicateRoundAndRole() {
        Long gameId = createGame();

        analysisFeedPersistenceService.appendFeedItems(gameId, List.of(
                item(gameId, 2, "Captain Erik Holm (Pilot)", "Rule-based", "First summary")
        ));
        AnalysisFeedResponseDto response = analysisFeedPersistenceService.appendFeedItems(gameId, List.of(
                item(gameId, 2, "Captain Erik Holm (Pilot)", "LLM", "Replacement summary"),
                item(gameId, 2, "Colonel Anna Sjöberg (Command / Operations)", "LLM", "Command summary")
        ));

        assertThat(response.items()).hasSize(2);
        assertThat(response.items())
                .extracting(AnalysisFeedItemDto::summary)
                .containsExactly("First summary", "Command summary");
    }

    private Long createGame() {
        GameSummaryDto summary = gameService.createGameFromScenario("SCN_STANDARD", "V7");
        return summary.gameId();
    }

    private AnalysisFeedItemDto item(Long gameId, int round, String role, String source, String summary) {
        return new AnalysisFeedItemDto(
                gameId + "-" + round + "-" + role,
                gameId,
                round,
                "ROUND_COMPLETE",
                role,
                source,
                summary,
                null,
                List.of("F1"),
                List.of("BASE_A"),
                "2026-03-11T12:00:00Z"
        );
    }
}
