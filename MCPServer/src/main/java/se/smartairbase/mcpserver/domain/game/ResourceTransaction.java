package se.smartairbase.mcpserver.domain.game;

import jakarta.persistence.*;
import se.smartairbase.mcpserver.domain.rule.enums.ResourceType;

import java.time.LocalDateTime;

@Entity
@Table(name = "resource_transaction")
public class ResourceTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "resource_transaction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_round_id")
    private GameRound gameRound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_base_id")
    private GameBase gameBase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_aircraft_id")
    private GameAircraft gameAircraft;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource", nullable = false, length = 30)
    private ResourceType resource;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "reason", nullable = false, length = 50)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected ResourceTransaction() {
    }

    public ResourceTransaction(Game game, GameRound gameRound, GameBase gameBase, GameAircraft gameAircraft,
                               ResourceType resource, Integer amount, String reason, LocalDateTime createdAt) {
        this.game = game;
        this.gameRound = gameRound;
        this.gameBase = gameBase;
        this.gameAircraft = gameAircraft;
        this.resource = resource;
        this.amount = amount;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Game getGame() { return game; }
    public GameRound getGameRound() { return gameRound; }
    public GameBase getGameBase() { return gameBase; }
    public GameAircraft getGameAircraft() { return gameAircraft; }
    public ResourceType getResource() { return resource; }
    public Integer getAmount() { return amount; }
    public String getReason() { return reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
