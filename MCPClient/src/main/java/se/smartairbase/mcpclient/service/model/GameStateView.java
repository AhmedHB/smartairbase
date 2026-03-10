package se.smartairbase.mcpclient.service.model;

import java.util.List;

public record GameStateView(
        GameSummaryView game,
        List<BaseStateView> bases,
        List<AircraftStateView> aircraft,
        List<MissionStateView> missions,
        long eventCount,
        long transactionCount
) {
}
