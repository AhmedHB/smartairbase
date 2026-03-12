package se.smartairbase.mcpclient.controller.dto;

public record ScenarioAircraftDTO(String code,
                                  String aircraftTypeCode,
                                  String startBaseCode,
                                  Integer fuelStart,
                                  Integer weaponsStart,
                                  Integer flightHoursStart) {
}
