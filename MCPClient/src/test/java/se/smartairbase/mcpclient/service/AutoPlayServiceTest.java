package se.smartairbase.mcpclient.service;

import org.junit.jupiter.api.Test;
import se.smartairbase.mcpclient.controller.dto.AircraftStateDTO;
import se.smartairbase.mcpclient.controller.dto.AssignMissionRequestDTO;
import se.smartairbase.mcpclient.controller.dto.DiceRollRequestDTO;
import se.smartairbase.mcpclient.controller.dto.GameStateDTO;
import se.smartairbase.mcpclient.controller.dto.GameSummaryDTO;
import se.smartairbase.mcpclient.controller.dto.LandAircraftRequestDTO;
import se.smartairbase.mcpclient.controller.dto.MissionStateDTO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutoPlayServiceTest {

    @Test
    void startNextRoundAssignsMissionsAutomatically() {
        SmartAirBaseMcpClient mcpClient = mock(SmartAirBaseMcpClient.class);
        GameRulesReferenceService rules = new GameRulesReferenceService();

        when(mcpClient.getGameStateView("7"))
                .thenReturn(state(activeSummary(0, null, false, true, false), readyAircraft("F1", "F2"), availableMissions("M1", "M3")))
                .thenReturn(state(activeSummary(1, "PLANNING", true, false, false), readyAircraft("F1", "F2"), availableMissions("M1", "M3")))
                .thenReturn(state(activeSummary(1, "DICE_ROLL", true, false, false), awaitingDiceAircraft("F1", "F2"), completedMissions("M1", "M3")))
                .thenReturn(state(activeSummary(1, "DICE_ROLL", true, false, false), awaitingDiceAircraft("F1", "F2"), completedMissions("M1", "M3")));

        AutoPlayService service = new AutoPlayService(mcpClient, rules);

        var response = service.startNextRound("7");

        verify(mcpClient).startRound("7");
        verify(mcpClient).assignMission(eq("7"), eq(new AssignMissionRequestDTO("F1", "M3")));
        verify(mcpClient).assignMission(eq("7"), eq(new AssignMissionRequestDTO("F2", "M1")));
        verify(mcpClient).resolveMissions("7");
        assertThat(response.pendingDiceAircraft()).containsExactly("F1", "F2");
        assertThat(response.nextAction()).isEqualTo("ROLL_DICE");
        assertThat(response.autoAssignments()).containsExactlyInAnyOrder("F1 -> M3", "F2 -> M1");
    }

    @Test
    void resolveDiceRollAutoLandsAndCompletesRound() {
        SmartAirBaseMcpClient mcpClient = mock(SmartAirBaseMcpClient.class);
        GameRulesReferenceService rules = new GameRulesReferenceService();

        when(mcpClient.getGameStateView("3"))
                .thenReturn(state(activeSummary(1, "LANDING", true, false, false), awaitingLandingAircraft("F1", "NONE"), completedMissions("M1")))
                .thenReturn(state(activeSummary(1, "LANDING", true, false, true), readyAircraft("F1"), completedMissions("M1")))
                .thenReturn(state(activeSummary(1, "LANDING", true, false, true), readyAircraft("F1"), completedMissions("M1")))
                .thenReturn(state(activeSummary(1, "ROUND_COMPLETE", false, true, false), readyAircraft("F1"), completedMissions("M1")));
        when(mcpClient.getLandingOptionsView("3", "F1"))
                .thenReturn(TestStateFactory.landingOptions("F1", false,
                        TestStateFactory.landingOption("A", true),
                        TestStateFactory.landingOption("B", true)));

        AutoPlayService service = new AutoPlayService(mcpClient, rules);

        var response = service.resolveDiceRoll("3", new DiceRollRequestDTO("F1", 1));

        verify(mcpClient).recordDiceRoll("3", new DiceRollRequestDTO("F1", 1));
        verify(mcpClient).landAircraft("3", new LandAircraftRequestDTO("F1", "A"));
        verify(mcpClient).completeRound("3");
        assertThat(response.roundCompleted()).isTrue();
        assertThat(response.nextAction()).isEqualTo("START_NEXT_ROUND");
        assertThat(response.autoLandings()).containsExactly("F1 -> A");
    }

    private GameStateDTO state(GameSummaryDTO summary,
                               List<AircraftStateDTO> aircraft,
                               List<MissionStateDTO> missions) {
        return TestStateFactory.state(summary, aircraft, missions);
    }

    private GameSummaryDTO activeSummary(Integer round, String phase, boolean roundOpen, boolean canStartRound,
                                         boolean canCompleteRound) {
        return TestStateFactory.summary(round, phase, roundOpen, canStartRound, canCompleteRound, "ACTIVE");
    }

    private List<AircraftStateDTO> readyAircraft(String... codes) {
        return java.util.Arrays.stream(codes)
                .map(code -> TestStateFactory.aircraft(code, "READY", "A", 100, 6, 20, "NONE"))
                .toList();
    }

    private List<AircraftStateDTO> awaitingDiceAircraft(String... codes) {
        return java.util.Arrays.stream(codes)
                .map(code -> TestStateFactory.aircraft(code, "AWAITING_DICE_ROLL", null, 70, 4, 12, "NONE"))
                .toList();
    }

    private List<AircraftStateDTO> awaitingLandingAircraft(String code, String damage) {
        return List.of(TestStateFactory.aircraft(code, "AWAITING_LANDING", null, 70, 4, 12, damage));
    }

    private List<MissionStateDTO> availableMissions(String... codes) {
        return java.util.Arrays.stream(codes)
                .map(code -> TestStateFactory.mission(code, "AVAILABLE"))
                .toList();
    }

    private List<MissionStateDTO> completedMissions(String... codes) {
        return java.util.Arrays.stream(codes)
                .map(code -> TestStateFactory.mission(code, "COMPLETED"))
                .toList();
    }
}
