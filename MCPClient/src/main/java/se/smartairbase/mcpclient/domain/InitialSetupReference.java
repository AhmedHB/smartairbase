package se.smartairbase.mcpclient.domain;

import java.util.List;

public record InitialSetupReference(
        int aircraftCount,
        List<String> baseCodes,
        int missionCount,
        int maxFuelPerAircraft,
        int maxWeaponsPerAircraft,
        int maxFlightHoursBeforeFullService,
        List<InitialAircraftReference> aircraft
) {
}
