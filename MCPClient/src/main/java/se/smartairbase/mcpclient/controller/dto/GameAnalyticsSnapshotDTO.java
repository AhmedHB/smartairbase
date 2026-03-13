package se.smartairbase.mcpclient.controller.dto;

/**
 * Browser-facing DTO for one finished-game analytics row.
 */
public record GameAnalyticsSnapshotDTO(
        Long gameAnalyticsSnapshotId,
        Long gameId,
        String gameName,
        String scenarioName,
        String gameStatus,
        boolean isWin,
        Integer roundsToOutcome,
        String diceSelectionProfile,
        Integer aircraftCount,
        Integer survivingAircraftCount,
        Integer destroyedAircraftCount,
        Integer missionCount,
        Integer completedMissionCount,
        Integer m1Count,
        Integer m2Count,
        Integer m3Count,
        String createdAt
) {
}
