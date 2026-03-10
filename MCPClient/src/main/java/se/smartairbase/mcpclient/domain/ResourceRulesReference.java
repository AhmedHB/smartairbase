package se.smartairbase.mcpclient.domain;

public record ResourceRulesReference(
        int holdingFuelCostPerRound,
        DeliveryScheduleReference fuelDeliveries,
        DeliveryScheduleReference sparePartDeliveries,
        DeliveryScheduleReference weaponDeliveries
) {
}
