package se.smartairbase.mcpclient.domain;

import java.util.Map;

public record DeliveryScheduleReference(
        int frequencyRounds,
        Map<String, Integer> deliveries
) {
}
