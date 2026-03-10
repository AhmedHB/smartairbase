package se.smartairbase.mcpclient.service.model;

public record MissionStateView(
        String code,
        String missionType,
        String status,
        int sortOrder,
        String assignmentBlocker
) {
}
