package se.smartairbase.mcpclient.domain;

public record InitialAircraftReference(
        String code,
        String baseCode,
        int fuel,
        int weapons,
        int flightHoursRemaining
) {
}
