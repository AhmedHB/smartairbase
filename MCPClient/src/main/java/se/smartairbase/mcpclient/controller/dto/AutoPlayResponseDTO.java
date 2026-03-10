package se.smartairbase.mcpclient.controller.dto;

import java.util.List;

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
