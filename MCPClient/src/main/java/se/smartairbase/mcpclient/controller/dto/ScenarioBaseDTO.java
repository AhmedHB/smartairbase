package se.smartairbase.mcpclient.controller.dto;

import java.util.List;

/**
 * Browser-facing DTO for one base entry in a scenario definition.
 */
public record ScenarioBaseDTO(String code,
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
                              List<ScenarioSupplyRuleDTO> supplyRules) {
}
