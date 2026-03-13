package se.smartairbase.mcpserver.mcp.dto;

/**
 * Describes one aircraft entry in a scenario definition payload.
 */
public record ScenarioAircraftDto(String code,
                                  String aircraftTypeCode,
                                  String startBaseCode,
                                  Integer fuelStart,
                                  Integer weaponsStart,
                                  Integer flightHoursStart) {
}
