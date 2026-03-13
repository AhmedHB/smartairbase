package se.smartairbase.mcpserver.mcp.dto;

/**
 * Read model for one base inside the aggregated game-state response.
 */
public record BaseStateDto(String code, String name, String baseType, int fuelStock, int weaponsStock,
                           int sparePartsStock, int occupiedParkingSlots, int parkingCapacity,
                           int occupiedMaintSlots, int maintenanceCapacity) {
}
