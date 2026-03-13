package se.smartairbase.mcpclient.domain;

import java.util.List;

/**
 * Immutable reference data for one base in the published rules summary.
 */
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
