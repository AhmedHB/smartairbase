package se.smartairbase.mcpserver.mcp.dto;

import java.util.List;

/**
 * Describes one base entry in a scenario definition payload.
 */
public record ScenarioBaseDto(String code,
                              String name,
                              String baseTypeCode,
                              Integer parkingCapacity,
                              Integer maintenanceCapacity,
                              Integer fuelStart,
                              Integer fuelMax,
                              Integer weaponsStart,
                              Integer weaponsMax,
                              Integer sparePartsStart,
                              Integer sparePartsMax,
                              List<ScenarioSupplyRuleDto> supplyRules) {
}
