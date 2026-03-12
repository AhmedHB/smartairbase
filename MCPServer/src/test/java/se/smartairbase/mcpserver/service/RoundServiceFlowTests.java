package se.smartairbase.mcpserver.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import se.smartairbase.mcpserver.domain.game.GameRound;
import se.smartairbase.mcpserver.mcp.dto.ActionResultDto;
import se.smartairbase.mcpserver.mcp.dto.AircraftStateDto;
import se.smartairbase.mcpserver.mcp.dto.GameStateDto;
import se.smartairbase.mcpserver.mcp.dto.GameSummaryDto;
import se.smartairbase.mcpserver.mcp.dto.LandingOptionsDto;
import se.smartairbase.mcpserver.mcp.dto.MissionStateDto;
import se.smartairbase.mcpserver.mcp.dto.RoundExecutionResultDto;
import se.smartairbase.mcpserver.repository.GameRoundRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.liquibase.clear-checksums=true"
})
@Transactional
class RoundServiceFlowTests {

    @Autowired
    private GameService gameService;

    @Autowired
    private RoundService roundService;

    @Autowired
    private GameQueryService gameQueryService;

    @Autowired
    private GameRoundRepository gameRoundRepository;

    @Test
    void startRoundOpensPlanningPhase() {
        Long gameId = gameService.createGameFromScenario("SCN_STANDARD", "V7").gameId();

        RoundExecutionResultDto result = roundService.startRound(gameId);
        GameStateDto state = gameQueryService.getGameState(gameId);

        assertThat(result.roundNumber()).isEqualTo(1);
        assertThat(result.phase()).isEqualTo("PLANNING");
        assertThat(result.roundOpen()).isTrue();

        GameSummaryDto summary = state.game();
        assertThat(summary.currentRound()).isEqualTo(1);
        assertThat(summary.roundPhase()).isEqualTo("PLANNING");
        assertThat(summary.roundOpen()).isTrue();
        assertThat(summary.canStartRound()).isFalse();
        assertThat(summary.canCompleteRound()).isFalse();
    }

    @Test
    void resolveMissionMovesAircraftToAwaitingDiceRoll() {
        Long gameId = gameService.createGameFromScenario("SCN_STANDARD", "V7").gameId();
        roundService.startRound(gameId);

        ActionResultDto assignment = roundService.assignMission(gameId, "F1", "M1-1");
        RoundExecutionResultDto result = roundService.resolveMissions(gameId);
        GameStateDto state = gameQueryService.getGameState(gameId);

        AircraftStateDto f1 = aircraft(state, "F1");
        MissionStateDto m1 = mission(state, "M1-1");

        assertThat(assignment.success()).isTrue();
        assertThat(result.phase()).isEqualTo("DICE_ROLL");
        assertThat(result.completedMissions()).containsExactly("M1-1");
        assertThat(result.pendingAircraft()).contains("F1");
        assertThat(f1.status()).isEqualTo("AWAITING_DICE_ROLL");
        assertThat(f1.fuel()).isEqualTo(80);
        assertThat(f1.weapons()).isEqualTo(6);
        assertThat(f1.remainingFlightHours()).isEqualTo(16);
        assertThat(m1.status()).isEqualTo("COMPLETED");
        assertThat(state.game().roundPhase()).isEqualTo("DICE_ROLL");
        assertThat(state.game().canCompleteRound()).isFalse();
    }

    @Test
    void resolveWithoutAssignmentsAllowsRoundToBeCompletedImmediately() {
        Long gameId = gameService.createGameFromScenario("SCN_STANDARD", "V7").gameId();
        roundService.startRound(gameId);

        RoundExecutionResultDto result = roundService.resolveMissions(gameId);
        GameStateDto state = gameQueryService.getGameState(gameId);

        assertThat(result.phase()).isEqualTo("LANDING");
        assertThat(result.pendingAircraft()).isEmpty();
        assertThat(state.game().roundPhase()).isEqualTo("LANDING");
        assertThat(state.game().canCompleteRound()).isTrue();
    }

    @Test
    void completeHappyPathRoundKeepsAircraftReadyAtLandingBase() {
        Long gameId = gameService.createGameFromScenario("SCN_STANDARD", "V7").gameId();
        roundService.startRound(gameId);
        roundService.assignMission(gameId, "F1", "M1-1");
        roundService.resolveMissions(gameId);

        ActionResultDto diceResult = roundService.recordDiceRoll(gameId, "F1", 6);
        LandingOptionsDto options = roundService.listAvailableLandingBases(gameId, "F1");
        ActionResultDto landingResult = roundService.landAircraft(gameId, "F1", "BASE_A");
        RoundExecutionResultDto completeResult = roundService.completeRound(gameId);
        GameStateDto state = gameQueryService.getGameState(gameId);

        AircraftStateDto f1 = aircraft(state, "F1");

        assertThat(diceResult.success()).isTrue();
        assertThat(options.holdingRequired()).isFalse();
        assertThat(options.options()).anyMatch(option -> option.baseCode().equals("BASE_A") && option.canLand());
        assertThat(landingResult.success()).isTrue();
        assertThat(completeResult.phase()).isEqualTo("ROUND_COMPLETE");
        assertThat(completeResult.roundOpen()).isFalse();
        assertThat(f1.status()).isEqualTo("READY");
        assertThat(f1.currentBase()).isEqualTo("BASE_A");
        assertThat(f1.lastDiceValue()).isEqualTo(6);
        assertThat(state.game().roundOpen()).isFalse();
        assertThat(state.game().canStartRound()).isTrue();
    }

    @Test
    void destroyedDiceOutcomeSkipsLandingAndMarksAircraftDestroyed() {
        Long gameId = gameService.createGameFromScenario("SCN_STANDARD", "V7").gameId();
        roundService.startRound(gameId);
        roundService.assignMission(gameId, "F1", "M1-1");
        roundService.resolveMissions(gameId);

        ActionResultDto diceResult = roundService.recordDiceRoll(gameId, "F1", 1);
        RoundExecutionResultDto completeResult = roundService.completeRound(gameId);
        GameStateDto state = gameQueryService.getGameState(gameId);
        AircraftStateDto f1 = aircraft(state, "F1");

        assertThat(diceResult.success()).isTrue();
        assertThat(f1.status()).isEqualTo("DESTROYED");
        assertThat(f1.damage()).isEqualTo("DESTROYED");
        assertThat(f1.currentBase()).isNull();
        assertThat(f1.lastDiceValue()).isEqualTo(1);
        assertThat(completeResult.phase()).isEqualTo("ROUND_COMPLETE");
        assertThat(state.game().roundOpen()).isFalse();
    }

    @Test
    void abortGameMarksGameAbortedAndClosesOpenRound() {
        Long gameId = gameService.createGameFromScenario("SCN_STANDARD", "V7").gameId();
        roundService.startRound(gameId);

        ActionResultDto abortResult = gameService.abortGame(gameId);
        GameStateDto state = gameQueryService.getGameState(gameId);
        GameRound round = gameRoundRepository.findByGame_IdOrderByRoundNumber(gameId).getFirst();

        assertThat(abortResult.success()).isTrue();
        assertThat(abortResult.message()).isEqualTo("Game aborted");
        assertThat(state.game().status()).isEqualTo("ABORTED");
        assertThat(state.game().roundOpen()).isFalse();
        assertThat(state.game().canStartRound()).isFalse();
        assertThat(round.getPhase().name()).isEqualTo("ROUND_COMPLETE");
        assertThat(round.getEndedAt()).isNotNull();
    }

    private AircraftStateDto aircraft(GameStateDto state, String code) {
        return state.aircraft().stream()
                .filter(aircraft -> aircraft.code().equals(code))
                .findFirst()
                .orElseThrow();
    }

    private MissionStateDto mission(GameStateDto state, String code) {
        return state.missions().stream()
                .filter(mission -> mission.code().equals(code))
                .findFirst()
                .orElseThrow();
    }
}
