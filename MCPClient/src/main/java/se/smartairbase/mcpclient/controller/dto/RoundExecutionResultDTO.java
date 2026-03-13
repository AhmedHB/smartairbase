package se.smartairbase.mcpclient.controller.dto;

import java.util.List;

/**
 * Browser-facing DTO summarizing one server-side round transition.
 */
public record RoundExecutionResultDTO(
        Long gameId,
        Integer roundNumber,
        String phase,
        String gameStatus,
        boolean roundOpen,
        List<String> pendingAircraft,
        List<String> affectedAircraft,
        List<String> completedMissions,
        List<String> supplyDeliveries,
        List<String> messages
) {
}
