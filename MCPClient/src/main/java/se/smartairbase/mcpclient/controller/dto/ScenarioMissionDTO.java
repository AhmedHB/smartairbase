package se.smartairbase.mcpclient.controller.dto;

public record ScenarioMissionDTO(String code,
                                 String missionTypeCode,
                                 String missionTypeName,
                                 Integer sortOrder,
                                 Integer defaultCount,
                                 Integer fuelCost,
                                 Integer weaponCost,
                                 Integer flightTimeCost) {
}
