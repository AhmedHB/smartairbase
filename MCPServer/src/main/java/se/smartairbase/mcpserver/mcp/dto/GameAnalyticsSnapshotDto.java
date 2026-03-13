package se.smartairbase.mcpserver.mcp.dto;

/**
 * Read model for one persisted analytics snapshot row from a finished game.
 */
public record GameAnalyticsSnapshotDto(
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
