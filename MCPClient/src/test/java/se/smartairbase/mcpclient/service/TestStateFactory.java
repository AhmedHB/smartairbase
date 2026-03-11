package se.smartairbase.mcpclient.service;

import se.smartairbase.mcpclient.controller.dto.AircraftStateDTO;
import se.smartairbase.mcpclient.controller.dto.BaseStateDTO;
import se.smartairbase.mcpclient.controller.dto.GameStateDTO;
import se.smartairbase.mcpclient.controller.dto.GameSummaryDTO;
import se.smartairbase.mcpclient.controller.dto.LandingOptionDTO;
import se.smartairbase.mcpclient.controller.dto.LandingOptionsDTO;
import se.smartairbase.mcpclient.controller.dto.MissionStateDTO;

import java.util.List;

public final class TestStateFactory {

    private TestStateFactory() {
    }

    public static GameSummaryDTO summary(Integer round, String phase, boolean roundOpen, boolean canStartRound,
                                         boolean canCompleteRound, String status) {
        return new GameSummaryDTO(1L, "test", "smartairbase", "7", status, round, phase, roundOpen, canStartRound, canCompleteRound);
    }

    public static AircraftStateDTO aircraft(String code, String status, String currentBase, int fuel, int weapons,
                                            int remainingFlightHours, String damage) {
        return new AircraftStateDTO(code, status, currentBase, fuel, weapons, remainingFlightHours,
                damage, 0, false, null, null, allowedActions(status));
    }

    public static MissionStateDTO mission(String code, String status) {
        return new MissionStateDTO(code, code, status, 1, null);
    }

    public static GameStateDTO state(GameSummaryDTO summary, List<AircraftStateDTO> aircraft, List<MissionStateDTO> missions) {
        return new GameStateDTO(summary, List.of(
                new BaseStateDTO("A", "Base A", "MAIN", 300, 20, 10, 0, 4, 0, 2),
                new BaseStateDTO("B", "Base B", "FORWARD", 200, 10, 4, 0, 2, 0, 1),
                new BaseStateDTO("C", "Base C", "FUEL", 150, 0, 0, 0, 2, 0, 0)
        ), aircraft, missions, 0, 0);
    }

    public static LandingOptionsDTO landingOptions(String aircraftCode, boolean holdingRequired, LandingOptionDTO... options) {
        return new LandingOptionsDTO(1L, 1, aircraftCode, holdingRequired, List.of(options));
    }

    public static LandingOptionDTO landingOption(String baseCode, boolean canLand) {
        return new LandingOptionDTO(baseCode, "Base " + baseCode, canLand, canLand ? "Landing available" : "Blocked");
    }

    public static BaseStateDTO base(String code, String name, int fuel, int weapons, int spareParts,
                                    int occupiedParkingSlots, int parkingCapacity, int occupiedMaintSlots, int maintenanceCapacity) {
        return new BaseStateDTO(code, name, "MAIN", fuel, weapons, spareParts,
                occupiedParkingSlots, parkingCapacity, occupiedMaintSlots, maintenanceCapacity);
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
