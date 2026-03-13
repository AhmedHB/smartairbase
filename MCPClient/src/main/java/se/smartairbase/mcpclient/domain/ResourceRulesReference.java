package se.smartairbase.mcpclient.domain;

/**
 * Immutable reference data for global resource and delivery rules.
 */
public record ResourceRulesReference(
        int holdingFuelCostPerRound,
        DeliveryScheduleReference fuelDeliveries,
        DeliveryScheduleReference sparePartDeliveries,
        DeliveryScheduleReference weaponDeliveries
) {
}
