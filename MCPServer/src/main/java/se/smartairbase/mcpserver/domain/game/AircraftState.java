package se.smartairbase.mcpserver.domain.game;

import jakarta.persistence.*;
import se.smartairbase.mcpserver.domain.rule.enums.DamageType;

@Entity
@Table(name = "aircraft_state")
/**
 * Mutable runtime state for one aircraft inside a specific game.
 *
 * <p>This entity contains operational values such as resources, current base,
 * damage outcome, repair timers and holding status.</p>
 */
public class AircraftState {

    @Id
    @Column(name = "game_aircraft_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "game_aircraft_id")
    private GameAircraft gameAircraft;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_base_id")
    private GameBase currentBase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_mission_id")
    private GameMission assignedMission;

    @Column(name = "fuel", nullable = false)
    private Integer fuel;

    @Column(name = "weapons", nullable = false)
    private Integer weapons;

    @Column(name = "remaining_flight_hours", nullable = false)
    private Integer remainingFlightHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "damage", nullable = false, length = 40)
    private DamageType damage = DamageType.NONE;

    @Column(name = "repair_rounds_remaining", nullable = false)
    private Integer repairRoundsRemaining = 0;

    @Column(name = "is_in_holding", nullable = false)
    private boolean inHolding = false;

    @Column(name = "holding_rounds", nullable = false)
    private Integer holdingRounds = 0;

    @Column(name = "last_dice_value")
    private Integer lastDiceValue;

    protected AircraftState() {
    }

    public AircraftState(GameAircraft gameAircraft, GameBase currentBase, Integer fuel, Integer weapons,
                         Integer remainingFlightHours) {
        this.gameAircraft = gameAircraft;
        this.currentBase = currentBase;
        this.fuel = fuel;
        this.weapons = weapons;
        this.remainingFlightHours = remainingFlightHours;
    }

    public void assignMission(GameMission assignedMission) {
        this.assignedMission = assignedMission;
    }

    public void clearAssignedMission() {
        this.assignedMission = null;
    }

    /**
     * Applies mission consumption to the aircraft after a mission has been
     * resolved. Values are clamped at zero to protect persistence invariants.
     */
    public void applyMissionCosts(int fuelCost, int weaponCost, int flightTimeCost) {
        this.fuel = Math.max(0, this.fuel - fuelCost);
        this.weapons = Math.max(0, this.weapons - weaponCost);
        this.remainingFlightHours = Math.max(0, this.remainingFlightHours - flightTimeCost);
    }

    public void setCurrentBase(GameBase currentBase) {
        this.currentBase = currentBase;
    }

    public void setFuel(Integer fuel) {
        this.fuel = fuel;
    }

    public void setWeapons(Integer weapons) {
        this.weapons = weapons;
    }

    public void setDamage(DamageType damage) {
        this.damage = damage;
    }

    public void setRepairRoundsRemaining(Integer repairRoundsRemaining) {
        this.repairRoundsRemaining = repairRoundsRemaining;
    }

    public void setInHolding(boolean inHolding) {
        this.inHolding = inHolding;
    }

    public void setHoldingRounds(Integer holdingRounds) {
        this.holdingRounds = holdingRounds;
    }

    public void setLastDiceValue(Integer lastDiceValue) {
        this.lastDiceValue = lastDiceValue;
    }

    public Long getId() { return id; }
    public GameAircraft getGameAircraft() { return gameAircraft; }
    public GameBase getCurrentBase() { return currentBase; }
    public GameMission getAssignedMission() { return assignedMission; }
    public Integer getFuel() { return fuel; }
    public Integer getWeapons() { return weapons; }
    public Integer getRemainingFlightHours() { return remainingFlightHours; }
    public DamageType getDamage() { return damage; }
    public Integer getRepairRoundsRemaining() { return repairRoundsRemaining; }
    public boolean isInHolding() { return inHolding; }
    public Integer getHoldingRounds() { return holdingRounds; }
    public Integer getLastDiceValue() { return lastDiceValue; }
}
