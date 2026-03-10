package se.smartairbase.mcpclient.controller.dto;

import java.util.List;

public record AircraftStateDTO(
        String code,
        String status,
        String currentBase,
        int fuel,
        int weapons,
        int remainingFlightHours,
        String damage,
        int repairRoundsRemaining,
        boolean inHolding,
        String assignedMission,
        Integer lastDiceValue,
        List<String> allowedActions
) {
}
