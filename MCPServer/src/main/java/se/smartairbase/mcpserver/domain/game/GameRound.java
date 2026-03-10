package se.smartairbase.mcpserver.domain.game;

import jakarta.persistence.*;
import se.smartairbase.mcpserver.domain.game.enums.RoundPhase;

import java.time.LocalDateTime;

@Entity
@Table(name = "game_round", uniqueConstraints = @UniqueConstraint(name = "uk_game_round", columnNames = {"game_id", "round_number"}))
/**
 * Persistent record of one round in a game.
 *
 * <p>The round is also the anchor for the phase-based state machine. A round is
 * considered open until {@code endedAt} is set.</p>
 */
public class GameRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_round_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false, length = 30)
    private RoundPhase phase;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    protected GameRound() {
    }

    public GameRound(Game game, Integer roundNumber, RoundPhase phase, LocalDateTime startedAt) {
        this.game = game;
        this.roundNumber = roundNumber;
        this.phase = phase;
        this.startedAt = startedAt;
    }

    public void setPhase(RoundPhase phase) {
        this.phase = phase;
    }

    public boolean isOpen() {
        return endedAt == null;
    }

    public void end(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public Long getId() { return id; }
    public Game getGame() { return game; }
    public Integer getRoundNumber() { return roundNumber; }
    public RoundPhase getPhase() { return phase; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
}
