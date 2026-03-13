package se.smartairbase.mcpserver.domain.game;

import jakarta.persistence.*;
import se.smartairbase.mcpserver.domain.game.enums.EventType;

import java.time.LocalDateTime;

@Entity
@Table(name = "game_event")
/**
 * Audit-style event row describing one important game transition.
 */
public class GameEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_event_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_round_id")
    private GameRound gameRound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_aircraft_id")
    private GameAircraft gameAircraft;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_base_id")
    private GameBase gameBase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_mission_id")
    private GameMission gameMission;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private EventType eventType;

    @Column(name = "details")
    private String details;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected GameEvent() {
    }

    public GameEvent(Game game, GameRound gameRound, GameAircraft gameAircraft, GameBase gameBase, GameMission gameMission,
                     EventType eventType, String details, LocalDateTime createdAt) {
        this.game = game;
        this.gameRound = gameRound;
        this.gameAircraft = gameAircraft;
        this.gameBase = gameBase;
        this.gameMission = gameMission;
        this.eventType = eventType;
        this.details = details;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Game getGame() { return game; }
    public GameRound getGameRound() { return gameRound; }
    public GameAircraft getGameAircraft() { return gameAircraft; }
    public GameBase getGameBase() { return gameBase; }
    public GameMission getGameMission() { return gameMission; }
    public EventType getEventType() { return eventType; }
    public String getDetails() { return details; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
