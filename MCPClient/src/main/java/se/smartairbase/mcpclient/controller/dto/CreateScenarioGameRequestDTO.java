package se.smartairbase.mcpclient.controller.dto;

import java.util.Map;

/**
 * HTTP request payload for creating one game from a chosen scenario definition.
 */
public record CreateScenarioGameRequestDTO(
        String gameName,
        Integer aircraftCount,
        Map<String, Integer> missionTypeCounts,
        Integer maxRounds
) {
}
