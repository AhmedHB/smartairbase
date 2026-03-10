package se.smartairbase.mcpclient.domain;

public record InventoryReference(
        int fuel,
        int weapons,
        int spareParts
) {
}
