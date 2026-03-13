package se.smartairbase.mcpclient.domain;

import java.util.List;

/**
 * Immutable reference data describing the default initial setup.
 */
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
