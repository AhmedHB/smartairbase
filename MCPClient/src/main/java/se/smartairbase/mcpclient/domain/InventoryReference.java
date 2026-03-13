package se.smartairbase.mcpclient.domain;

/**
 * Immutable reference data for a fuel, weapons, and spare-parts inventory set.
 */
public record InventoryReference(
        int fuel,
        int weapons,
        int spareParts
) {
}
