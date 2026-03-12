package se.smartairbase.mcpserver.mcp.dto;

public record ScenarioMissionDto(String code,
                                 String missionTypeCode,
                                 String missionTypeName,
                                 Integer sortOrder,
                                 Integer defaultCount,
                                 Integer fuelCost,
                                 Integer weaponCost,
                                 Integer flightTimeCost) {
}
