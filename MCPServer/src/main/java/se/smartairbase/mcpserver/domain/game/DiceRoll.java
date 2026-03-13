package se.smartairbase.mcpserver.domain.game;

import jakarta.persistence.*;
import se.smartairbase.mcpserver.domain.game.enums.DiceSelectionMode;
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

    /**
     * Exact selection method for this roll, used for later analytics and audit.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "dice_selection_mode", nullable = false, length = 40)
    private DiceSelectionMode diceSelectionMode;

    @Column(name = "rolled_at", nullable = false)
    private LocalDateTime rolledAt;

    protected DiceRoll() {
    }

    public DiceRoll(GameRound gameRound, GameAircraft gameAircraft, Integer diceValue, RepairRule repairRule,
                    DiceSelectionMode diceSelectionMode, LocalDateTime rolledAt) {
        this.gameRound = gameRound;
        this.gameAircraft = gameAircraft;
        this.diceValue = diceValue;
        this.repairRule = repairRule;
        this.diceSelectionMode = diceSelectionMode;
        this.rolledAt = rolledAt;
    }

    public Long getId() { return id; }
    public GameRound getGameRound() { return gameRound; }
    public GameAircraft getGameAircraft() { return gameAircraft; }
    public Integer getDiceValue() { return diceValue; }
    public RepairRule getRepairRule() { return repairRule; }
    public DiceSelectionMode getDiceSelectionMode() { return diceSelectionMode; }
    public LocalDateTime getRolledAt() { return rolledAt; }
}
