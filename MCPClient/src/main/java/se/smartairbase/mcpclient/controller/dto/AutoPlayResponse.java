package se.smartairbase.mcpclient.controller.dto;

import se.smartairbase.mcpclient.service.model.GameStateView;

import java.util.List;

public record AutoPlayResponse(
        GameStateView gameState,
        String nextAction,
        boolean roundCompleted,
        boolean gameFinished,
        List<String> pendingDiceAircraft,
        List<String> autoAssignments,
        List<String> autoLandings,
        List<String> messages
) {
}
