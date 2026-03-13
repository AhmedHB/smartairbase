package se.smartairbase.mcpclient.controller.dto;

/**
 * Browser-facing DTO for one mission row in the current game state.
 */
public record MissionStateDTO(
        String code,
        String missionType,
        String status,
        int sortOrder,
        String assignmentBlocker
) {
}
