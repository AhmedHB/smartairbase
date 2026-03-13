package se.smartairbase.mcpserver.domain.game;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import se.smartairbase.mcpserver.domain.game.enums.SimulationBatchGameStatus;

import java.time.LocalDateTime;

/**
 * Tracks one generated game inside a simulator batch, including its run number and lifecycle status.
 */
@Entity
@Table(name = "simulation_batch_game")
public class SimulationBatchGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "simulation_batch_game_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "simulation_batch_id", nullable = false)
    private SimulationBatch simulationBatch;

    @Column(name = "game_id")
    private Long gameId;

    @Column(name = "run_number", nullable = false)
    private Integer runNumber;

    @Column(name = "game_name", nullable = false, length = 120)
    private String gameName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SimulationBatchGameStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    protected SimulationBatchGame() {
    }

    public SimulationBatchGame(SimulationBatch simulationBatch, Integer runNumber, String gameName,
                               SimulationBatchGameStatus status, LocalDateTime createdAt) {
        this.simulationBatch = simulationBatch;
        this.runNumber = runNumber;
        this.gameName = gameName;
        this.status = status;
        this.createdAt = createdAt;
    }

    public void markRunning(Long gameId) {
        this.gameId = gameId;
        this.status = SimulationBatchGameStatus.RUNNING;
    }

    public void markCompleted(LocalDateTime now) {
        this.status = SimulationBatchGameStatus.COMPLETED;
        this.endedAt = now;
    }

    public void markFailed(LocalDateTime now) {
        this.status = SimulationBatchGameStatus.FAILED;
        this.endedAt = now;
    }

    public Long getId() { return id; }
    public SimulationBatch getSimulationBatch() { return simulationBatch; }
    public Long getGameId() { return gameId; }
    public Integer getRunNumber() { return runNumber; }
    public String getGameName() { return gameName; }
    public SimulationBatchGameStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
}
