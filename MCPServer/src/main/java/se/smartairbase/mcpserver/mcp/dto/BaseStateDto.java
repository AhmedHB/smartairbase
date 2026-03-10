package se.smartairbase.mcpserver.mcp.dto;

public record BaseStateDto(String code, String name, String baseType, int fuelStock, int weaponsStock,
                           int sparePartsStock, int occupiedParkingSlots, int parkingCapacity,
                           int occupiedMaintSlots, int maintenanceCapacity) {
}
