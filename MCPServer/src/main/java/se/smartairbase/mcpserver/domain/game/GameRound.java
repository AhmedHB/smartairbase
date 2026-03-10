package se.smartairbase.mcpserver.domain.game;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "game_round", uniqueConstraints = @UniqueConstraint(name = "uk_game_round", columnNames = {"game_id", "round_number"}))
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

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    protected GameRound() {
    }

    public GameRound(Game game, Integer roundNumber, LocalDateTime startedAt) {
        this.game = game;
        this.roundNumber = roundNumber;
        this.startedAt = startedAt;
    }

    public void end(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public Long getId() { return id; }
    public Game getGame() { return game; }
    public Integer getRoundNumber() { return roundNumber; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
}
