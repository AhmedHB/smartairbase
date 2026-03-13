package se.smartairbase.mcpclient.controller.dto;

/**
 * Browser-facing DTO for one mission entry in a scenario definition.
 */
public record ScenarioMissionDTO(String code,
                                 String missionTypeCode,
                                 String missionTypeName,
                                 Integer sortOrder,
                                 Integer defaultCount,
                                 Integer fuelCost,
                                 Integer weaponCost,
                                 Integer flightTimeCost) {
}
