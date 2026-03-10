package se.smartairbase.mcpclient.service.model;

import java.util.List;

public record AircraftStateView(
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
