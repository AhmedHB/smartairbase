package se.smartairbase.mcpclient.controller.dto;

/**
 * HTTP request payload for creating one game from a chosen scenario definition.
 */
public record CreateScenarioGameRequestDTO(String gameName) {
}
