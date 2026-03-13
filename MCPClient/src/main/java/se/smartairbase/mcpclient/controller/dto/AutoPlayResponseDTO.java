package se.smartairbase.mcpclient.controller.dto;

import java.util.List;

/**
 * Browser-facing DTO describing one automated-play step result.
 */
public record AutoPlayResponseDTO(
        GameStateDTO gameState,
        String nextAction,
        boolean roundCompleted,
        boolean gameFinished,
        List<String> pendingDiceAircraft,
        List<String> autoAssignments,
        List<String> autoLandings,
        List<String> messages
) {
}
