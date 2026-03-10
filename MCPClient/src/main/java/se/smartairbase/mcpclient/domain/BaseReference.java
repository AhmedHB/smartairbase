package se.smartairbase.mcpclient.domain;

import java.util.List;

public record BaseReference(
        String code,
        String name,
        int parkingSlots,
        int maintenanceSlots,
        InventoryReference startingInventory,
        InventoryReference maxInventory,
        List<String> capabilities
) {
}
