package se.smartairbase.mcpclient.domain;

public record MissionReference(
        String code,
        String name,
        int flightHours,
        int fuelCost,
        int weaponCost
) {
}
