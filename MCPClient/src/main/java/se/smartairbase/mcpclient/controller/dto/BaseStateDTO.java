package se.smartairbase.mcpclient.controller.dto;

/**
 * Browser-facing DTO for one base row in the current game state.
 */
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
