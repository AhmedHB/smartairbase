package se.smartairbase.mcpserver.domain.rule;

import jakarta.persistence.*;
import se.smartairbase.mcpserver.domain.rule.enums.DamageType;

@Entity
@Table(name = "repair_rule")
/**
 * Dice-to-damage mapping rule used during post-mission damage resolution.
 */
public class RepairRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "repair_rule_id")
    private Long id;

    @Column(name = "dice_value", nullable = false, unique = true)
    private Integer diceValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "damage", nullable = false, length = 40)
    private DamageType damage;

    @Column(name = "spare_parts_cost", nullable = false)
    private Integer sparePartsCost;

    @Column(name = "repair_rounds", nullable = false)
    private Integer repairRounds;

    @Column(name = "requires_full_service", nullable = false)
    private boolean requiresFullService;

    protected RepairRule() {
    }

    public RepairRule(Integer diceValue, DamageType damage, Integer sparePartsCost, Integer repairRounds, boolean requiresFullService) {
        this.diceValue = diceValue;
        this.damage = damage;
        this.sparePartsCost = sparePartsCost;
        this.repairRounds = repairRounds;
        this.requiresFullService = requiresFullService;
    }

    public Long getId() { return id; }
    public Integer getDiceValue() { return diceValue; }
    public DamageType getDamage() { return damage; }
    public Integer getSparePartsCost() { return sparePartsCost; }
    public Integer getRepairRounds() { return repairRounds; }
    public boolean isRequiresFullService() { return requiresFullService; }
}
