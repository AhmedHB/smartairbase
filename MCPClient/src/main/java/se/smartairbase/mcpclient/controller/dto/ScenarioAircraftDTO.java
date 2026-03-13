package se.smartairbase.mcpclient.controller.dto;

/**
 * Browser-facing DTO for one aircraft entry in a scenario definition.
 */
public record ScenarioAircraftDTO(String code,
                                  String aircraftTypeCode,
                                  String startBaseCode,
                                  Integer fuelStart,
                                  Integer weaponsStart,
                                  Integer flightHoursStart) {
}
