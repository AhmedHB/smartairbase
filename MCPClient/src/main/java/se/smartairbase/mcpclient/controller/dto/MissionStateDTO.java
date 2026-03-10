package se.smartairbase.mcpclient.controller.dto;

public record MissionStateDTO(
        String code,
        String missionType,
        String status,
        int sortOrder,
        String assignmentBlocker
) {
}
