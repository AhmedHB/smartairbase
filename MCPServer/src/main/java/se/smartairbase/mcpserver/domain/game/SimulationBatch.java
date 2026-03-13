package se.smartairbase.mcpserver.domain.game;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import se.smartairbase.mcpserver.domain.game.enums.SimulationBatchStatus;

import java.time.LocalDateTime;

/**
 * Persists one simulator request and its aggregate execution status across all generated games.
 */
@Entity
@Table(name = "simulation_batch")
public class SimulationBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "simulation_batch_id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "scenario_name", nullable = false, length = 100)
    private String scenarioName;

    @Column(name = "aircraft_count", nullable = false)
    private Integer aircraftCount;

    @Column(name = "m1_count", nullable = false)
    private Integer m1Count;

    @Column(name = "m2_count", nullable = false)
    private Integer m2Count;

    @Column(name = "m3_count", nullable = false)
    private Integer m3Count;

    @Column(name = "dice_strategy", nullable = false, length = 40)
    private String diceStrategy;

    @Column(name = "max_rounds", nullable = false)
    private Integer maxRounds;

    @Column(name = "requested_runs", nullable = false)
    private Integer requestedRuns;

    @Column(name = "completed_runs", nullable = false)
    private Integer completedRuns = 0;

    @Column(name = "failed_runs", nullable = false)
    private Integer failedRuns = 0;

    @Column(name = "won_runs", nullable = false)
    private Integer wonRuns = 0;

    @Column(name = "lost_runs", nullable = false)
    private Integer lostRuns = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SimulationBatchStatus status = SimulationBatchStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    protected SimulationBatch() {
    }

    public SimulationBatch(String name, String scenarioName, Integer aircraftCount, Integer m1Count, Integer m2Count,
                           Integer m3Count, String diceStrategy, Integer maxRounds, Integer requestedRuns, LocalDateTime createdAt) {
        this.name = name;
        this.scenarioName = scenarioName;
        this.aircraftCount = aircraftCount;
        this.m1Count = m1Count;
        this.m2Count = m2Count;
        this.m3Count = m3Count;
        this.diceStrategy = diceStrategy;
        this.maxRounds = maxRounds;
        this.requestedRuns = requestedRuns;
        this.createdAt = createdAt;
    }

    public void markRunning(LocalDateTime now) {
        this.status = SimulationBatchStatus.RUNNING;
        this.startedAt = now;
    }

    public void incrementCompletedRuns() {
        this.completedRuns += 1;
    }

    public void incrementFailedRuns() {
        this.failedRuns += 1;
    }

    public void incrementWonRuns() {
        this.wonRuns += 1;
    }

    public void incrementLostRuns() {
        this.lostRuns += 1;
    }

    public void markCompleted(LocalDateTime now) {
        this.status = SimulationBatchStatus.COMPLETED;
        this.endedAt = now;
    }

    public void markFailed(LocalDateTime now) {
        this.status = SimulationBatchStatus.FAILED;
        this.endedAt = now;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getScenarioName() { return scenarioName; }
    public Integer getAircraftCount() { return aircraftCount; }
    public Integer getM1Count() { return m1Count; }
    public Integer getM2Count() { return m2Count; }
    public Integer getM3Count() { return m3Count; }
    public String getDiceStrategy() { return diceStrategy; }
    public Integer getMaxRounds() { return maxRounds; }
    public Integer getRequestedRuns() { return requestedRuns; }
    public Integer getCompletedRuns() { return completedRuns; }
    public Integer getFailedRuns() { return failedRuns; }
    public Integer getWonRuns() { return wonRuns; }
    public Integer getLostRuns() { return lostRuns; }
    public SimulationBatchStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
}
