package se.smartairbase.mcpclient.domain;

/**
 * Immutable reference data for one mission type in the published rules summary.
 */
public record MissionReference(
        String code,
        String name,
        int flightHours,
        int fuelCost,
        int weaponCost
) {
}
