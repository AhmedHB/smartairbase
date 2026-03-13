package se.smartairbase.mcpserver.mcp.dto;

/**
 * Describes one mission entry in a scenario definition payload.
 */
public record ScenarioMissionDto(String code,
                                 String missionTypeCode,
                                 String missionTypeName,
                                 Integer sortOrder,
                                 Integer defaultCount,
                                 Integer fuelCost,
                                 Integer weaponCost,
                                 Integer flightTimeCost) {
}
