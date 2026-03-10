package se.smartairbase.mcpclient.service;

import se.smartairbase.mcpclient.service.model.AircraftStateView;
import se.smartairbase.mcpclient.service.model.BaseStateView;
import se.smartairbase.mcpclient.service.model.GameStateView;
import se.smartairbase.mcpclient.service.model.GameSummaryView;
import se.smartairbase.mcpclient.service.model.LandingOptionView;
import se.smartairbase.mcpclient.service.model.LandingOptionsView;
import se.smartairbase.mcpclient.service.model.MissionStateView;

import java.util.List;

public final class TestStateFactory {

    private TestStateFactory() {
    }

    public static GameSummaryView summary(Integer round, String phase, boolean roundOpen, boolean canStartRound,
                                          boolean canCompleteRound, String status) {
        return new GameSummaryView(1L, "test", "smartairbase", "7", status, round, phase, roundOpen, canStartRound, canCompleteRound);
    }

    public static AircraftStateView aircraft(String code, String status, String currentBase, int fuel, int weapons,
                                             int remainingFlightHours, String damage) {
        return new AircraftStateView(code, status, currentBase, fuel, weapons, remainingFlightHours,
                damage, 0, false, null, null, allowedActions(status));
    }

    public static MissionStateView mission(String code, String status) {
        return new MissionStateView(code, code, status, 1, null);
    }

    public static GameStateView state(GameSummaryView summary, List<AircraftStateView> aircraft, List<MissionStateView> missions) {
        return new GameStateView(summary, List.of(
                new BaseStateView("A", "Base A", "MAIN", 300, 20, 10, 0, 4, 0, 2),
                new BaseStateView("B", "Base B", "FORWARD", 200, 10, 4, 0, 2, 0, 1),
                new BaseStateView("C", "Base C", "FUEL", 150, 0, 0, 0, 2, 0, 0)
        ), aircraft, missions, 0, 0);
    }

    public static LandingOptionsView landingOptions(String aircraftCode, boolean holdingRequired, LandingOptionView... options) {
        return new LandingOptionsView(1L, 1, aircraftCode, holdingRequired, List.of(options));
    }

    public static LandingOptionView landingOption(String baseCode, boolean canLand) {
        return new LandingOptionView(baseCode, "Base " + baseCode, canLand, canLand ? "Landing available" : "Blocked");
    }

    private static List<String> allowedActions(String status) {
        return switch (status) {
            case "READY" -> List.of("ASSIGN_MISSION");
            case "AWAITING_DICE_ROLL" -> List.of("RECORD_DICE_ROLL");
            case "AWAITING_LANDING" -> List.of("LAND", "SEND_TO_HOLDING");
            default -> List.of();
        };
    }
}
