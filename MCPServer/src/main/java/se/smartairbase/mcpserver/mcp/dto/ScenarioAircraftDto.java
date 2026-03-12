package se.smartairbase.mcpserver.mcp.dto;

public record ScenarioAircraftDto(String code,
                                  String aircraftTypeCode,
                                  String startBaseCode,
                                  Integer fuelStart,
                                  Integer weaponsStart,
                                  Integer flightHoursStart) {
}
