package se.smartairbase.mcpserver.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import se.smartairbase.mcpserver.domain.game.SimulationBatch;
import se.smartairbase.mcpserver.domain.game.SimulationBatchGame;
import se.smartairbase.mcpserver.domain.game.enums.SimulationBatchGameStatus;
import se.smartairbase.mcpserver.mcp.dto.AircraftStateDto;
import se.smartairbase.mcpserver.mcp.dto.GameStateDto;
import se.smartairbase.mcpserver.mcp.dto.GameSummaryDto;
import se.smartairbase.mcpserver.mcp.dto.LandingOptionDto;
import se.smartairbase.mcpserver.mcp.dto.LandingOptionsDto;
import se.smartairbase.mcpserver.mcp.dto.MissionStateDto;
import se.smartairbase.mcpserver.repository.SimulationBatchGameRepository;
import se.smartairbase.mcpserver.repository.SimulationBatchRepository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes simulator batches asynchronously by creating ordinary games and driving them to a terminal state.
 * A run is only marked completed when the underlying game actually reaches WON or LOST.
 */
@Service
public class SimulationBatchRunner {

    private enum SimulationRunOutcome {
        WON,
        LOST,
        NON_TERMINAL
    }

    private final SimulationBatchRepository simulationBatchRepository;
    private final SimulationBatchGameRepository simulationBatchGameRepository;
    private final GameService gameService;
    private final GameQueryService gameQueryService;
    private final RoundService roundService;

    public SimulationBatchRunner(SimulationBatchRepository simulationBatchRepository,
                                 SimulationBatchGameRepository simulationBatchGameRepository,
                                 GameService gameService,
                                 GameQueryService gameQueryService,
                                 RoundService roundService) {
        this.simulationBatchRepository = simulationBatchRepository;
        this.simulationBatchGameRepository = simulationBatchGameRepository;
        this.gameService = gameService;
        this.gameQueryService = gameQueryService;
        this.roundService = roundService;
    }

    @Async
    public void runBatch(Long batchId) {
        SimulationBatch batch = simulationBatchRepository.findById(batchId).orElseThrow();
        batch.markRunning(LocalDateTime.now());
        simulationBatchRepository.save(batch);

        try {
          for (int runNumber = 1; runNumber <= batch.getRequestedRuns(); runNumber++) {
              String gameName = batch.getName() + "_" + String.format("%03d", runNumber);
              SimulationBatchGame batchGame = simulationBatchGameRepository.save(
                      new SimulationBatchGame(batch, runNumber, gameName, SimulationBatchGameStatus.CREATED, LocalDateTime.now())
              );
              try {
                  Map<String, Integer> missionCounts = new LinkedHashMap<>();
                  missionCounts.put("M1", batch.getM1Count());
                  missionCounts.put("M2", batch.getM2Count());
                  missionCounts.put("M3", batch.getM3Count());

                  GameSummaryDto createdGame = gameService.createGameFromScenario(
                          batch.getScenarioName(),
                          "",
                          gameName,
                          batch.getAircraftCount(),
                          missionCounts,
                          batch.getMaxRounds()
                  );
                  batchGame.markRunning(createdGame.gameId());
                  simulationBatchGameRepository.save(batchGame);

                  SimulationRunOutcome outcome = runSingleSimulation(createdGame.gameId(), batch.getDiceStrategy(), batch.getMaxRounds());
                  if (outcome == SimulationRunOutcome.WON || outcome == SimulationRunOutcome.LOST) {
                      batchGame.markCompleted(LocalDateTime.now());
                      batch.incrementCompletedRuns();
                  } else {
                      batchGame.markFailed(LocalDateTime.now());
                      batch.incrementFailedRuns();
                  }
                  simulationBatchGameRepository.save(batchGame);
                  simulationBatchRepository.save(batch);
              } catch (Exception exception) {
                  batchGame.markFailed(LocalDateTime.now());
                  batch.incrementFailedRuns();
                  simulationBatchGameRepository.save(batchGame);
                  simulationBatchRepository.save(batch);
              }
          }

          if (batch.getCompletedRuns() == 0 && batch.getFailedRuns() > 0) {
              batch.markFailed(LocalDateTime.now());
          } else {
              batch.markCompleted(LocalDateTime.now());
          }
          simulationBatchRepository.save(batch);
        } catch (Exception exception) {
            batch.markFailed(LocalDateTime.now());
            simulationBatchRepository.save(batch);
        }
    }

    private SimulationRunOutcome runSingleSimulation(Long gameId, String diceStrategy, Integer maxRounds) {
        GameStateDto state = gameQueryService.getGameState(gameId);
        int safetyCounter = 0;
        int maxSimulationRounds = Math.max(1, maxRounds == null ? 1000 : maxRounds);
        while ("ACTIVE".equals(state.game().status()) && safetyCounter < maxSimulationRounds) {
            safetyCounter += 1;
            if (state.game().canStartRound()) {
                roundService.startRound(gameId);
                assignMissionsGreedily(gameId);
                roundService.resolveMissions(gameId);
            }

            state = gameQueryService.getGameState(gameId);
            if (!"ACTIVE".equals(state.game().status())) {
                break;
            }

            if ("DICE_ROLL".equals(state.game().roundPhase())) {
                for (AircraftStateDto aircraft : state.aircraft()) {
                    if ("AWAITING_DICE_ROLL".equals(aircraft.status())) {
                        roundService.recordDiceRoll(gameId, aircraft.code(), automatedDiceValue(diceStrategy), automatedDiceSelectionMode(diceStrategy));
                    }
                }
            }

            state = gameQueryService.getGameState(gameId);
            if ("LANDING".equals(state.game().roundPhase())) {
                for (AircraftStateDto aircraft : state.aircraft()) {
                    if (!"AWAITING_LANDING".equals(aircraft.status())) {
                        continue;
                    }
                    LandingOptionsDto options = roundService.listAvailableLandingBases(gameId, aircraft.code());
                    LandingOptionDto landing = options.options().stream().filter(LandingOptionDto::canLand).findFirst().orElse(null);
                    if (landing != null) {
                        roundService.landAircraft(gameId, aircraft.code(), landing.baseCode());
                    } else {
                        roundService.sendAircraftToHolding(gameId, aircraft.code());
                    }
                }
            }

            state = gameQueryService.getGameState(gameId);
            if (state.game().roundOpen() && state.game().canCompleteRound()) {
                roundService.completeRound(gameId);
            }
            state = gameQueryService.getGameState(gameId);
        }

        if ("WON".equals(state.game().status())) {
            return SimulationRunOutcome.WON;
        }
        if ("LOST".equals(state.game().status())) {
            return SimulationRunOutcome.LOST;
        }
        return SimulationRunOutcome.NON_TERMINAL;
    }

    private void assignMissionsGreedily(Long gameId) {
        GameStateDto state = gameQueryService.getGameState(gameId);
        List<MissionStateDto> missions = state.missions().stream()
                .filter(mission -> "AVAILABLE".equals(mission.status()))
                .sorted((left, right) -> Integer.compare(right.sortOrder(), left.sortOrder()))
                .toList();

        for (MissionStateDto mission : missions) {
            AircraftStateDto aircraft = state.aircraft().stream()
                    .filter(candidate -> "READY".equals(candidate.status()))
                    .filter(candidate -> candidate.allowedActions() != null && candidate.allowedActions().contains("ASSIGN_MISSION"))
                    .findFirst()
                    .orElse(null);
            if (aircraft == null) {
                return;
            }
            roundService.assignMission(gameId, aircraft.code(), mission.code());
            state = gameQueryService.getGameState(gameId);
        }
    }

    private int automatedDiceValue(String strategy) {
        if ("MIN_DAMAGE".equals(strategy)) {
            return List.of(4, 5, 6).get((int) (Math.random() * 3));
        }
        if ("MAX_DAMAGE".equals(strategy)) {
            return List.of(1, 2, 3).get((int) (Math.random() * 3));
        }
        return 1 + (int) (Math.random() * 6);
    }

    private String automatedDiceSelectionMode(String strategy) {
        if ("MIN_DAMAGE".equals(strategy)) {
            return "AUTO_MIN_DAMAGE";
        }
        if ("MAX_DAMAGE".equals(strategy)) {
            return "AUTO_MAX_DAMAGE";
        }
        return "AUTO_RANDOM";
    }
}
