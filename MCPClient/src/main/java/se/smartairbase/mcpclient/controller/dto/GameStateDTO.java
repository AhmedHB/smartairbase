package se.smartairbase.mcpclient.controller.dto;

import java.util.List;

/**
 * Browser-facing DTO for the full observable state of one game.
 */
public record GameStateDTO(
        GameSummaryDTO game,
        List<BaseStateDTO> bases,
        List<AircraftStateDTO> aircraft,
        List<MissionStateDTO> missions,
        long eventCount,
        long transactionCount
) {
}
