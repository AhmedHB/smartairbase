package se.smartairbase.mcpserver.mcp.dto;

public record AircraftStateDto(String code, String status, String currentBase, int fuel, int weapons,
                               int remainingFlightHours, String damage, int repairRoundsRemaining,
                               boolean inHolding, String assignedMission) {
}
