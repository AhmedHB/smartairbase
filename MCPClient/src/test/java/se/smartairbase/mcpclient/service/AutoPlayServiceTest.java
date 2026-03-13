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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
                .thenReturn(state(activeSummary(1, "DICE_ROLL", true, false, false), awaitingDiceAircraft("F1"), completedMissions("M1")))
                .thenReturn(state(activeSummary(1, "LANDING", true, false, false), awaitingLandingAircraft("F1", "NONE"), completedMissions("M1")))
                .thenReturn(state(activeSummary(1, "LANDING", true, false, true), readyAircraft("F1"), completedMissions("M1")))
                .thenReturn(state(activeSummary(1, "LANDING", true, false, true), readyAircraft("F1"), completedMissions("M1")))
                .thenReturn(state(activeSummary(1, "ROUND_COMPLETE", false, true, false), readyAircraft("F1"), completedMissions("M1")));
        when(mcpClient.getLandingOptionsView("3", "F1"))
                .thenReturn(TestStateFactory.landingOptions("F1", false,
                        TestStateFactory.landingOption("A", true),
                        TestStateFactory.landingOption("B", true)));

        AutoPlayService service = new AutoPlayService(mcpClient, rules);

        var response = service.resolveDiceRoll("3", new DiceRollRequestDTO("F1", 6, "AUTO_RANDOM"));

        verify(mcpClient).recordDiceRoll("3", new DiceRollRequestDTO("F1", 6, "AUTO_RANDOM"));
        verify(mcpClient).landAircraft("3", new LandAircraftRequestDTO("F1", "A"));
        verify(mcpClient).completeRound("3");
        assertThat(response.roundCompleted()).isTrue();
        assertThat(response.nextAction()).isEqualTo("START_NEXT_ROUND");
        assertThat(response.autoLandings()).containsExactly("F1 -> A");
    }

    @Test
    void planNextRoundStopsInPlanningForManualFlow() {
        SmartAirBaseMcpClient mcpClient = mock(SmartAirBaseMcpClient.class);
        GameRulesReferenceService rules = new GameRulesReferenceService();

        when(mcpClient.getGameStateView("7"))
                .thenReturn(state(activeSummary(0, null, false, true, false), readyAircraft("F1", "F2"), availableMissions("M1", "M3")))
                .thenReturn(state(activeSummary(1, "PLANNING", true, false, false), readyAircraft("F1", "F2"), availableMissions("M1", "M3")))
                .thenReturn(state(activeSummary(1, "PLANNING", true, false, false), readyAircraft("F1", "F2"), availableMissions("M1", "M3")));

        AutoPlayService service = new AutoPlayService(mcpClient, rules);

        var response = service.planNextRound("7");

        verify(mcpClient).startRound("7");
        verify(mcpClient).assignMission(eq("7"), eq(new AssignMissionRequestDTO("F1", "M3")));
        verify(mcpClient).assignMission(eq("7"), eq(new AssignMissionRequestDTO("F2", "M1")));
        verify(mcpClient, never()).resolveMissions("7");
        assertThat(response.nextAction()).isEqualTo("RESOLVE_MISSIONS");
        assertThat(response.autoAssignments()).containsExactlyInAnyOrder("F1 -> M3", "F2 -> M1");
    }

    @Test
    void resolvePlannedMissionsMovesManualFlowToDice() {
        SmartAirBaseMcpClient mcpClient = mock(SmartAirBaseMcpClient.class);
        GameRulesReferenceService rules = new GameRulesReferenceService();

        when(mcpClient.getGameStateView("5"))
                .thenReturn(state(activeSummary(1, "PLANNING", true, false, false), readyAircraft("F1"), availableMissions("M1")))
                .thenReturn(state(activeSummary(1, "DICE_ROLL", true, false, false), awaitingDiceAircraft("F1"), completedMissions("M1")))
                .thenReturn(state(activeSummary(1, "DICE_ROLL", true, false, false), awaitingDiceAircraft("F1"), completedMissions("M1")));

        AutoPlayService service = new AutoPlayService(mcpClient, rules);

        var response = service.resolvePlannedMissions("5");

        verify(mcpClient).resolveMissions("5");
        assertThat(response.nextAction()).isEqualTo("ROLL_DICE");
        assertThat(response.pendingDiceAircraft()).containsExactly("F1");
    }

    @Test
    void resolveDiceRollReturnsCurrentStateWhenRoundAlreadyMovedToLanding() {
        SmartAirBaseMcpClient mcpClient = mock(SmartAirBaseMcpClient.class);
        GameRulesReferenceService rules = new GameRulesReferenceService();

        when(mcpClient.getGameStateView("9"))
                .thenReturn(state(activeSummary(1, "DICE_ROLL", true, false, false), awaitingDiceAircraft("F1"), completedMissions("M1")))
                .thenReturn(state(activeSummary(1, "LANDING", true, false, false), awaitingLandingAircraft("F1", "NONE"), completedMissions("M1")));
        when(mcpClient.recordDiceRoll("9", new DiceRollRequestDTO("F1", 1, "AUTO_MIN_DAMAGE")))
                .thenThrow(new IllegalStateException("Round is in phase LANDING"));
        when(mcpClient.getLandingOptionsView("9", "F1"))
                .thenReturn(TestStateFactory.landingOptions("F1", false,
                        TestStateFactory.landingOption("BASE_A", true)));

        AutoPlayService service = new AutoPlayService(mcpClient, rules);

        var response = service.resolveDiceRoll("9", new DiceRollRequestDTO("F1", 1, "AUTO_MIN_DAMAGE"));

        assertThat(response.nextAction()).isEqualTo("WAIT");
        assertThat(response.messages()).contains("Dice step already finished");
    }

    @Test
    void resolveDiceRollDoesNotAttemptLandingForDestroyedAircraft() {
        SmartAirBaseMcpClient mcpClient = mock(SmartAirBaseMcpClient.class);
        GameRulesReferenceService rules = new GameRulesReferenceService();

        when(mcpClient.getGameStateView("11"))
                .thenReturn(state(activeSummary(1, "DICE_ROLL", true, false, false), awaitingDiceAircraft("F1"), completedMissions("M1")))
                .thenReturn(state(activeSummary(1, "LANDING", true, false, true),
                        List.of(TestStateFactory.aircraft("F1", "DESTROYED", null, 70, 4, 12, "DESTROYED")),
                        completedMissions("M1")))
                .thenReturn(state(activeSummary(1, "LANDING", true, false, true),
                        List.of(TestStateFactory.aircraft("F1", "DESTROYED", null, 70, 4, 12, "DESTROYED")),
                        completedMissions("M1")))
                .thenReturn(state(activeSummary(1, "ROUND_COMPLETE", false, true, false),
                        List.of(TestStateFactory.aircraft("F1", "DESTROYED", null, 70, 4, 12, "DESTROYED")),
                        completedMissions("M1")));

        AutoPlayService service = new AutoPlayService(mcpClient, rules);

        var response = service.resolveDiceRoll("11", new DiceRollRequestDTO("F1", 1, "AUTO_MIN_DAMAGE"));

        verify(mcpClient).recordDiceRoll("11", new DiceRollRequestDTO("F1", 1, "AUTO_MIN_DAMAGE"));
        verify(mcpClient, never()).getLandingOptionsView("11", "F1");
        verify(mcpClient, never()).landAircraft(eq("11"), argThat(request -> "F1".equals(request.aircraftCode())));
        verify(mcpClient).completeRound("11");
        assertThat(response.roundCompleted()).isTrue();
        assertThat(response.messages()).contains("F1 destroyed");
    }

    @Test
    void resolveDiceRollNormalizesBaseCodesForLandingChoice() {
        SmartAirBaseMcpClient mcpClient = mock(SmartAirBaseMcpClient.class);
        GameRulesReferenceService rules = new GameRulesReferenceService();

        when(mcpClient.getGameStateView("3"))
                .thenReturn(state(activeSummary(1, "DICE_ROLL", true, false, false), awaitingDiceAircraft("F1"), completedMissions("M1")))
                .thenReturn(stateWithBases(activeSummary(1, "LANDING", true, false, false),
                        awaitingLandingAircraft("F1", "NONE"),
                        completedMissions("M1"),
                        List.of(
                                TestStateFactory.base("BASE_A", "Base A", 300, 20, 10, 0, 4, 0, 2),
                                TestStateFactory.base("BASE_B", "Base B", 200, 10, 4, 0, 2, 0, 1),
                                TestStateFactory.base("BASE_C", "Base C", 150, 0, 0, 0, 2, 0, 0)
                        )))
                .thenReturn(stateWithBases(activeSummary(1, "LANDING", true, false, true),
                        readyAircraft("F1"),
                        completedMissions("M1"),
                        List.of(
                                TestStateFactory.base("BASE_A", "Base A", 280, 18, 10, 1, 4, 0, 2),
                                TestStateFactory.base("BASE_B", "Base B", 200, 10, 4, 0, 2, 0, 1),
                                TestStateFactory.base("BASE_C", "Base C", 150, 0, 0, 0, 2, 0, 0)
                        )))
                .thenReturn(state(activeSummary(1, "ROUND_COMPLETE", false, true, false), readyAircraft("F1"), completedMissions("M1")));
        when(mcpClient.getLandingOptionsView("3", "F1"))
                .thenReturn(TestStateFactory.landingOptions("F1", false,
                        TestStateFactory.landingOption("BASE_A", true),
                        TestStateFactory.landingOption("BASE_B", true)));

        AutoPlayService service = new AutoPlayService(mcpClient, rules);

        service.resolveDiceRoll("3", new DiceRollRequestDTO("F1", 6, "AUTO_RANDOM"));

        verify(mcpClient).landAircraft(eq("3"), argThat(request -> "F1".equals(request.aircraftCode()) && "BASE_A".equals(request.baseCode())));
    }

    private GameStateDTO state(GameSummaryDTO summary,
                               List<AircraftStateDTO> aircraft,
                               List<MissionStateDTO> missions) {
        return TestStateFactory.state(summary, aircraft, missions);
    }

    private GameStateDTO stateWithBases(GameSummaryDTO summary,
                                        List<AircraftStateDTO> aircraft,
                                        List<MissionStateDTO> missions,
                                        List<se.smartairbase.mcpclient.controller.dto.BaseStateDTO> bases) {
        return new GameStateDTO(summary, bases, aircraft, missions, 0, 0);
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
