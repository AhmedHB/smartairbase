package se.smartairbase.mcpclient.controller.dto;

public record GameSummaryDTO(
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
