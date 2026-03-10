package se.smartairbase.mcpclient.controller.dto;

public record BaseStateDTO(
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
