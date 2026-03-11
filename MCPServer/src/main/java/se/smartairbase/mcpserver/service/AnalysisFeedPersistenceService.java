package se.smartairbase.mcpserver.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.smartairbase.mcpserver.domain.game.Game;
import se.smartairbase.mcpserver.domain.game.GameAnalysisEntry;
import se.smartairbase.mcpserver.mcp.dto.AnalysisFeedItemDto;
import se.smartairbase.mcpserver.mcp.dto.AnalysisFeedResponseDto;
import se.smartairbase.mcpserver.repository.GameAnalysisEntryRepository;
import se.smartairbase.mcpserver.repository.GameRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AnalysisFeedPersistenceService {

    private static final String LIST_SEPARATOR = "|";

    private final GameRepository gameRepository;
    private final GameAnalysisEntryRepository gameAnalysisEntryRepository;

    public AnalysisFeedPersistenceService(GameRepository gameRepository,
                                          GameAnalysisEntryRepository gameAnalysisEntryRepository) {
        this.gameRepository = gameRepository;
        this.gameAnalysisEntryRepository = gameAnalysisEntryRepository;
    }

    @Transactional(readOnly = true)
    public AnalysisFeedResponseDto listFeed(Long gameId) {
        // The server owns the persisted feed so a restarted MCPClient can reload
        // the full commentary history for a game.
        List<AnalysisFeedItemDto> items = gameAnalysisEntryRepository.findByGame_IdOrderByCreatedAtAscIdAsc(gameId).stream()
                .map(this::toDto)
                .toList();
        Integer lastRound = items.stream()
                .map(AnalysisFeedItemDto::round)
                .filter(round -> round != null)
                .max(Integer::compareTo)
                .orElse(null);
        return new AnalysisFeedResponseDto(items, false, lastRound);
    }

    @Transactional
    public AnalysisFeedResponseDto appendFeedItems(Long gameId, List<AnalysisFeedItemDto> items) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));

        for (AnalysisFeedItemDto item : items) {
            if (item == null || item.round() == null || item.role() == null || item.summary() == null) {
                continue;
            }
            // One saved entry per game/round/role keeps feed generation idempotent.
            if (gameAnalysisEntryRepository.existsByGame_IdAndRoundNumberAndRoleName(gameId, item.round(), item.role())) {
                continue;
            }
            gameAnalysisEntryRepository.save(new GameAnalysisEntry(
                    game,
                    item.round(),
                    item.phase(),
                    item.role(),
                    item.source() != null ? item.source() : "Rule-based",
                    item.summary(),
                    item.details(),
                    join(item.relatedAircraft()),
                    join(item.relatedBases()),
                    LocalDateTime.now()
            ));
        }

        return listFeed(gameId);
    }

    private AnalysisFeedItemDto toDto(GameAnalysisEntry entry) {
        return new AnalysisFeedItemDto(
                String.valueOf(entry.getId()),
                entry.getGame().getId(),
                entry.getRoundNumber(),
                entry.getPhase(),
                entry.getRoleName(),
                entry.getSource(),
                entry.getSummary(),
                entry.getDetails(),
                split(entry.getRelatedAircraft()),
                split(entry.getRelatedBases()),
                entry.getCreatedAt().toString()
        );
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        // Persist list-shaped metadata as a stable, compact delimited string.
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted()
                .reduce((left, right) -> left + LIST_SEPARATOR + right)
                .orElse(null);
    }

    private List<String> split(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.split("\\|"));
    }
}
