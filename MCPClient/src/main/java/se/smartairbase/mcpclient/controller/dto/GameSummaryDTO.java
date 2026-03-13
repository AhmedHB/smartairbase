package se.smartairbase.mcpclient.controller.dto;

/**
 * Browser-facing DTO for top-level game status and control flags.
 */
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
        boolean canCompleteRound,
        Integer maxRounds
) {
}
