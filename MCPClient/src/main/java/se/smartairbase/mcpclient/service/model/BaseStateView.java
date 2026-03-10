package se.smartairbase.mcpclient.service.model;

public record BaseStateView(
        String code,
        String name,
        String baseType,
        int fuelStock,
        int weaponsStock,
        int sparePartsStock,
        int occupiedParkingSlots,
        int parkingCapacity,
        int occupiedMaintSlots,
        int maintenanceCapacity
) {
}
