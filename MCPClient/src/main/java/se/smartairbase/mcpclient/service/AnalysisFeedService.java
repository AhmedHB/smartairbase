package se.smartairbase.mcpclient.service;

import org.springframework.stereotype.Service;
import se.smartairbase.mcpclient.controller.dto.AnalysisFeedItemDTO;
import se.smartairbase.mcpclient.controller.dto.AnalysisFeedResponseDTO;
import se.smartairbase.mcpclient.controller.dto.GameAnalyticsSnapshotDTO;
import se.smartairbase.mcpclient.controller.dto.GameStateDTO;
import se.smartairbase.mcpclient.controller.dto.GameSummaryResponseDTO;
import se.smartairbase.mcpclient.domain.AnalysisRole;
import se.smartairbase.mcpclient.service.analysis.AnalysisFactService;
import se.smartairbase.mcpclient.service.analysis.AnalysisGameFacts;
import se.smartairbase.mcpclient.service.analysis.AnalysisNarration;
import se.smartairbase.mcpclient.service.analysis.AnalysisNarrationService;
import se.smartairbase.mcpclient.service.analysis.AnalysisRoundFacts;
import se.smartairbase.mcpclient.service.analysis.AnalysisFactService.Snapshot;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
/**
 * Coordinates persisted analysis-feed loading and per-round narration generation.
 */
public class AnalysisFeedService {

    private final SmartAirBaseMcpClient mcpClient;
    private final AnalysisFactService analysisFactService;
    private final AnalysisNarrationService analysisNarrationService;
    private final Map<String, Snapshot> snapshotByGameId = new ConcurrentHashMap<>();

    public AnalysisFeedService(SmartAirBaseMcpClient mcpClient,
                               AnalysisFactService analysisFactService,
                               AnalysisNarrationService analysisNarrationService) {
        this.mcpClient = mcpClient;
        this.analysisFactService = analysisFactService;
        this.analysisNarrationService = analysisNarrationService;
    }

    public AnalysisFeedResponseDTO getFeed(String gameId) {
        return mcpClient.listAnalysisFeed(gameId);
    }

    public AnalysisFeedResponseDTO generateRoundAnalysis(String gameId) {
        GameStateDTO currentState = mcpClient.getGameStateView(gameId);
        AnalysisFeedResponseDTO existingFeed = mcpClient.listAnalysisFeed(gameId);
        if (currentState.game().currentRound() == null || currentState.game().currentRound() <= 0) {
            return existingFeed;
        }
        if (Objects.equals(existingFeed.lastAnalyzedRound(), currentState.game().currentRound())) {
            return existingFeed;
        }

        List<AnalysisFeedItemDTO> newItems = buildItems(currentState, snapshotByGameId.get(gameId));
        // Narration is generated in the client, but the saved feed history lives in
        // MCPServer so it survives client restarts and page reloads.
        AnalysisFeedResponseDTO persistedFeed = mcpClient.appendAnalysisFeedItems(gameId, newItems);
        // The diff snapshot remains client-local for now and only helps shape the
        // next round's narration; it is not the persisted history.
        snapshotByGameId.put(gameId, analysisFactService.snapshot(currentState));
        return persistedFeed;
    }

    public GameSummaryResponseDTO generateFinalAnalysis(String gameId) {
        GameAnalyticsSnapshotDTO snapshot = mcpClient.getGameAnalyticsSnapshot(gameId);
        if (snapshot == null) {
            return new GameSummaryResponseDTO(null, List.of());
        }
        AnalysisGameFacts facts = new AnalysisGameFacts(
                snapshot.gameId(),
                snapshot.gameStatus(),
                snapshot.roundsToOutcome() != null ? snapshot.roundsToOutcome() : 0,
                snapshot.completedMissionCount() != null ? snapshot.completedMissionCount() : 0,
                snapshot.missionCount() != null ? snapshot.missionCount() : 0,
                snapshot.survivingAircraftCount() != null ? snapshot.survivingAircraftCount() : 0,
                snapshot.destroyedAircraftCount() != null ? snapshot.destroyedAircraftCount() : 0,
                snapshot.diceSelectionProfile()
        );
        String createdAt = OffsetDateTime.now().toString();
        List<AnalysisFeedItemDTO> items = List.of(
                finalItem(gameId, AnalysisRole.PILOT, facts, createdAt),
                finalItem(gameId, AnalysisRole.GROUND_CREW, facts, createdAt),
                finalItem(gameId, AnalysisRole.MAINTENANCE_TECHNICIANS, facts, createdAt),
                finalItem(gameId, AnalysisRole.COMMAND_OPERATIONS, facts, createdAt)
        );
        mcpClient.appendAnalysisFeedItems(gameId, items);
        return new GameSummaryResponseDTO(snapshot, items);
    }

    public GameSummaryResponseDTO getGameSummary(String gameId) {
        GameAnalyticsSnapshotDTO snapshot = mcpClient.getGameAnalyticsSnapshot(gameId);
        AnalysisFeedResponseDTO feed = mcpClient.listAnalysisFeed(gameId);
        List<AnalysisFeedItemDTO> finalFeed = (feed.items() == null ? List.<AnalysisFeedItemDTO>of() : feed.items()).stream()
                .filter(item -> item.round() != null && item.round() == 0)
                .toList();
        return new GameSummaryResponseDTO(snapshot, finalFeed);
    }

    private AnalysisFeedItemDTO finalItem(String gameId, AnalysisRole role, AnalysisGameFacts facts, String createdAt) {
        AnalysisNarration narration = analysisNarrationService.narrateFinal(role, facts);
        return new AnalysisFeedItemDTO(
                gameId + "-0-" + role.name(),
                facts.gameId(),
                0,
                "GAME_SUMMARY",
                role.displayName(),
                narration.source(),
                narration.summary(),
                narration.details(),
                List.of(),
                List.of(),
                createdAt
        );
    }

    private List<AnalysisFeedItemDTO> buildItems(GameStateDTO currentState, Snapshot previousSnapshot) {
        AnalysisRoundFacts facts = analysisFactService.buildFacts(currentState, previousSnapshot);
        String createdAt = OffsetDateTime.now().toString();
        Integer round = facts.round();
        String phase = facts.phase();
        Long gameId = facts.gameId();

        return List.of(
                item(gameId, round, phase, AnalysisRole.PILOT,
                        facts,
                        facts.landedAircraft(),
                        List.of(),
                        createdAt),
                item(gameId, round, phase, AnalysisRole.GROUND_CREW,
                        facts,
                        combine(facts.refueledAircraft(), facts.rearmedAircraft()),
                        facts.affectedBases(),
                        createdAt),
                item(gameId, round, phase, AnalysisRole.MAINTENANCE_TECHNICIANS,
                        facts,
                        combine(facts.maintenanceAircraft(), facts.fullServiceAircraft()),
                        facts.affectedBases(),
                        createdAt),
                item(gameId, round, phase, AnalysisRole.COMMAND_OPERATIONS,
                        facts,
                        facts.maintenanceAircraft(),
                        facts.keyBases(),
                        createdAt)
        );
    }

    private AnalysisFeedItemDTO item(Long gameId, Integer round, String phase, AnalysisRole role,
                                     AnalysisRoundFacts facts, List<String> relatedAircraft,
                                     List<String> relatedBases, String createdAt) {
        AnalysisNarration narration = analysisNarrationService.narrate(role, facts);
        return new AnalysisFeedItemDTO(
                gameId + "-" + round + "-" + role.name(),
                gameId,
                round,
                phase,
                role.displayName(),
                narration.source(),
                narration.summary(),
                narration.details(),
                relatedAircraft,
                relatedBases,
                createdAt
        );
    }

    private List<String> combine(List<String> left, List<String> right) {
        List<String> combined = new ArrayList<>(left);
        combined.addAll(right);
        return combined.stream().distinct().sorted().toList();
    }
}
