package se.smartairbase.mcpclient.service.analysis;

/**
 * Structured facts about the entire completed game, used as input for final narration generation.
 */
public record AnalysisGameFacts(
        Long gameId,
        String gameStatus,
        int totalRounds,
        int completedMissions,
        int totalMissions,
        int survivingAircraftCount,
        int destroyedAircraftCount,
        String diceSelectionProfile
) {
}
