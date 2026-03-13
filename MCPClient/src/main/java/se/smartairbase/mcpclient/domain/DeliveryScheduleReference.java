package se.smartairbase.mcpclient.domain;

import java.util.Map;

/**
 * Immutable reference data for one recurring resource delivery schedule.
 */
public record DeliveryScheduleReference(
        int frequencyRounds,
        Map<String, Integer> deliveries
) {
}
