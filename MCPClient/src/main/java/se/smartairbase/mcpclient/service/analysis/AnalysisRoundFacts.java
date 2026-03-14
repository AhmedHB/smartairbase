package se.smartairbase.mcpclient.service.analysis;

import java.util.List;

/**
 * Structured facts used as the input contract for narration generation.
 */
public record AnalysisRoundFacts(
        Long gameId,
        Integer round,
        String phase,
        String gameStatus,
        String scenarioName,
        Integer maxRounds,
        int completedMissions,
        int totalMissions,
        int missionsRemaining,
        int readyAircraftCount,
        int destroyedAircraftCount,
        List<String> landedAircraft,
        List<String> holdingAircraft,
        List<String> destroyedThisRound,
        List<String> maintenanceAircraft,
        List<String> underRepairAircraft,
        List<String> awaitingRepairAircraft,
        List<String> fullServiceAircraft,
        List<String> refueledAircraft,
        List<String> rearmedAircraft,
        List<String> affectedBases,
        List<String> keyBases
) {
}
