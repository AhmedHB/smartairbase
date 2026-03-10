package se.smartairbase.mcpclient.controller.dto;

import java.util.List;

public record GameStateDTO(
        GameSummaryDTO game,
        List<BaseStateDTO> bases,
        List<AircraftStateDTO> aircraft,
        List<MissionStateDTO> missions,
        long eventCount,
        long transactionCount
) {
}
