package se.smartairbase.mcpserver.domain.game;

import jakarta.persistence.*;
import se.smartairbase.mcpserver.domain.game.enums.GameStatus;
import se.smartairbase.mcpserver.domain.rule.Scenario;

import java.time.LocalDateTime;

@Entity
@Table(name = "game")
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Column(name = "name", length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GameStatus status = GameStatus.NOT_STARTED;

    @Column(name = "current_round", nullable = false)
    private Integer currentRound = 0;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    protected Game() {
    }

    public Game(Scenario scenario, String name) {
        this.scenario = scenario;
        this.name = name;
    }

    public void markActive(LocalDateTime now) {
        this.status = GameStatus.ACTIVE;
        this.startedAt = now;
    }

    public void setCurrentRound(Integer currentRound) {
        this.currentRound = currentRound;
    }

    public void markWon(LocalDateTime now) {
        this.status = GameStatus.WON;
        this.endedAt = now;
    }

    public void markLost(LocalDateTime now) {
        this.status = GameStatus.LOST;
        this.endedAt = now;
    }

    public Long getId() { return id; }
    public Scenario getScenario() { return scenario; }
    public String getName() { return name; }
    public GameStatus getStatus() { return status; }
    public Integer getCurrentRound() { return currentRound; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
}
