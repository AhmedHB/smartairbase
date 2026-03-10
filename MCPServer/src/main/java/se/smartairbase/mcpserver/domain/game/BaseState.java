package se.smartairbase.mcpserver.domain.game;

import jakarta.persistence.*;

@Entity
@Table(name = "base_state")
/**
 * Mutable runtime state for a base inside one game.
 *
 * <p>This entity tracks stocks and currently occupied capacities. Immutable
 * base limits remain on {@link GameBase}.</p>
 */
public class BaseState {

    @Id
    @Column(name = "game_base_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "game_base_id")
    private GameBase gameBase;

    @Column(name = "fuel_stock", nullable = false)
    private Integer fuelStock;

    @Column(name = "weapons_stock", nullable = false)
    private Integer weaponsStock;

    @Column(name = "spare_parts_stock", nullable = false)
    private Integer sparePartsStock;

    @Column(name = "occupied_parking_slots", nullable = false)
    private Integer occupiedParkingSlots;

    @Column(name = "occupied_maint_slots", nullable = false)
    private Integer occupiedMaintSlots;

    protected BaseState() {
    }

    public BaseState(GameBase gameBase, Integer fuelStock, Integer weaponsStock, Integer sparePartsStock,
                     Integer occupiedParkingSlots, Integer occupiedMaintSlots) {
        this.gameBase = gameBase;
        this.fuelStock = fuelStock;
        this.weaponsStock = weaponsStock;
        this.sparePartsStock = sparePartsStock;
        this.occupiedParkingSlots = occupiedParkingSlots;
        this.occupiedMaintSlots = occupiedMaintSlots;
    }

    public void addFuel(int amount, int max) {
        this.fuelStock = Math.min(max, this.fuelStock + amount);
    }

    public void addWeapons(int amount, int max) {
        this.weaponsStock = Math.min(max, this.weaponsStock + amount);
    }

    public void addSpareParts(int amount, int max) {
        this.sparePartsStock = Math.min(max, this.sparePartsStock + amount);
    }

    public void consumeFuel(int amount) { this.fuelStock = Math.max(0, this.fuelStock - amount); }
    public void consumeWeapons(int amount) { this.weaponsStock = Math.max(0, this.weaponsStock - amount); }
    public void consumeSpareParts(int amount) { this.sparePartsStock = Math.max(0, this.sparePartsStock - amount); }
    public void incrementOccupiedParkingSlots() { this.occupiedParkingSlots = this.occupiedParkingSlots + 1; }
    public void decrementOccupiedParkingSlots() { this.occupiedParkingSlots = Math.max(0, this.occupiedParkingSlots - 1); }
    public void incrementOccupiedMaintSlots() { this.occupiedMaintSlots = this.occupiedMaintSlots + 1; }
    public void decrementOccupiedMaintSlots() { this.occupiedMaintSlots = Math.max(0, this.occupiedMaintSlots - 1); }
    public void setOccupiedParkingSlots(Integer occupiedParkingSlots) { this.occupiedParkingSlots = occupiedParkingSlots; }
    public void setOccupiedMaintSlots(Integer occupiedMaintSlots) { this.occupiedMaintSlots = occupiedMaintSlots; }

    public Long getId() { return id; }
    public GameBase getGameBase() { return gameBase; }
    public Integer getFuelStock() { return fuelStock; }
    public Integer getWeaponsStock() { return weaponsStock; }
    public Integer getSparePartsStock() { return sparePartsStock; }
    public Integer getOccupiedParkingSlots() { return occupiedParkingSlots; }
    public Integer getOccupiedMaintSlots() { return occupiedMaintSlots; }
}
