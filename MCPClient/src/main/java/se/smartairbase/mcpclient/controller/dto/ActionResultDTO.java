package se.smartairbase.mcpclient.controller.dto;

/**
 * Browser-facing DTO for simple success or failure command results.
 */
public record ActionResultDTO(boolean success, String message) {
}
