package se.smartairbase.mcpserver.domain.game;

import jakarta.persistence.*;
import se.smartairbase.mcpserver.domain.rule.RepairRule;

import java.time.LocalDateTime;

@Entity
@Table(name = "dice_roll", uniqueConstraints = @UniqueConstraint(name = "uk_dice_roll_round_aircraft", columnNames = {"game_round_id", "game_aircraft_id"}))
public class DiceRoll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dice_roll_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_round_id", nullable = false)
    private GameRound gameRound;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_aircraft_id", nullable = false)
    private GameAircraft gameAircraft;

    @Column(name = "dice_value", nullable = false)
    private Integer diceValue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repair_rule_id", nullable = false)
    private RepairRule repairRule;

    @Column(name = "rolled_at", nullable = false)
    private LocalDateTime rolledAt;

    protected DiceRoll() {
    }

    public DiceRoll(GameRound gameRound, GameAircraft gameAircraft, Integer diceValue, RepairRule repairRule,
                    LocalDateTime rolledAt) {
        this.gameRound = gameRound;
        this.gameAircraft = gameAircraft;
        this.diceValue = diceValue;
        this.repairRule = repairRule;
        this.rolledAt = rolledAt;
    }

    public Long getId() { return id; }
    public GameRound getGameRound() { return gameRound; }
    public GameAircraft getGameAircraft() { return gameAircraft; }
    public Integer getDiceValue() { return diceValue; }
    public RepairRule getRepairRule() { return repairRule; }
    public LocalDateTime getRolledAt() { return rolledAt; }
}
