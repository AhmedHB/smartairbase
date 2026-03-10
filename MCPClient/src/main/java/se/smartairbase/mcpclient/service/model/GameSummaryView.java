package se.smartairbase.mcpclient.service.model;

public record GameSummaryView(
        Long gameId,
        String name,
        String scenarioName,
        String scenarioVersion,
        String status,
        Integer currentRound,
        String roundPhase,
        boolean roundOpen,
        boolean canStartRound,
        boolean canCompleteRound
) {
}
