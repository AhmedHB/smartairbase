package se.smartairbase.mcpclient.domain;

/**
 * Immutable reference data for one aircraft in the initial setup summary.
 */
public record InitialAircraftReference(
        String code,
        String baseCode,
        int fuel,
        int weapons,
        int flightHoursRemaining
) {
}
