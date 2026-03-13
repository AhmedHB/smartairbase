package se.smartairbase.mcpserver.domain.game;

import jakarta.persistence.*;
import se.smartairbase.mcpserver.domain.game.enums.DiceSelectionProfile;
import se.smartairbase.mcpserver.domain.game.enums.GameStatus;
import se.smartairbase.mcpserver.domain.rule.Scenario;

import java.time.LocalDateTime;

@Entity
@Table(name = "game")
/**
 * Root aggregate for one playable game instance.
 *
 * <p>The game tracks high-level lifecycle such as active/won/lost status and
 * the latest round number. Detailed mutable state is stored in child tables.</p>
 */
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

    @Column(name = "max_rounds", nullable = false)
    private Integer maxRounds = 1000;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    /**
     * Derived summary of the dice-selection styles that have occurred so far in
     * this game.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "dice_selection_profile", length = 40)
    private DiceSelectionProfile diceSelectionProfile;

    protected Game() {
    }

    public Game(Scenario scenario, String name, Integer maxRounds) {
        this.scenario = scenario;
        this.name = name;
        this.maxRounds = maxRounds == null ? 1000 : Math.max(1, maxRounds);
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

    /**
     * Marks the game as explicitly aborted by a client or operator action.
     */
    public void markAborted(LocalDateTime now) {
        this.status = GameStatus.ABORTED;
        this.endedAt = now;
    }

    public void setDiceSelectionProfile(DiceSelectionProfile diceSelectionProfile) {
        this.diceSelectionProfile = diceSelectionProfile;
    }

    public Long getId() { return id; }
    public Scenario getScenario() { return scenario; }
    public String getName() { return name; }
    public GameStatus getStatus() { return status; }
    public Integer getCurrentRound() { return currentRound; }
    public Integer getMaxRounds() { return maxRounds; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public DiceSelectionProfile getDiceSelectionProfile() { return diceSelectionProfile; }
}
