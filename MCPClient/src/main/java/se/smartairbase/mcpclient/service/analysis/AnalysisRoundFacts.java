package se.smartairbase.mcpclient.service.analysis;

import java.util.List;

public record AnalysisRoundFacts(
        Long gameId,
        Integer round,
        String phase,
        String gameStatus,
        int completedMissions,
        int totalMissions,
        int readyAircraftCount,
        int destroyedAircraftCount,
        List<String> landedAircraft,
        List<String> holdingAircraft,
        List<String> destroyedThisRound,
        List<String> maintenanceAircraft,
        List<String> fullServiceAircraft,
        List<String> refueledAircraft,
        List<String> rearmedAircraft,
        List<String> affectedBases,
        List<String> keyAircraft,
        List<String> keyBases
) {
}
