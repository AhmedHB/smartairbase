package se.smartairbase.mcpserver.domain.game;

import jakarta.persistence.*;
import se.smartairbase.mcpserver.domain.rule.BaseType;

@Entity
@Table(name = "game_base", uniqueConstraints = @UniqueConstraint(name = "uk_game_base_code", columnNames = {"game_id", "code"}))
public class GameBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_base_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "base_type_id", nullable = false)
    private BaseType baseType;

    @Column(name = "parking_capacity", nullable = false)
    private Integer parkingCapacity;

    @Column(name = "maintenance_capacity", nullable = false)
    private Integer maintenanceCapacity;

    @Column(name = "fuel_max", nullable = false)
    private Integer fuelMax;

    @Column(name = "weapons_max", nullable = false)
    private Integer weaponsMax;

    @Column(name = "spare_parts_max", nullable = false)
    private Integer sparePartsMax;

    protected GameBase() {
    }

    public GameBase(Game game, String code, String name, BaseType baseType, Integer parkingCapacity,
                    Integer maintenanceCapacity, Integer fuelMax, Integer weaponsMax, Integer sparePartsMax) {
        this.game = game;
        this.code = code;
        this.name = name;
        this.baseType = baseType;
        this.parkingCapacity = parkingCapacity;
        this.maintenanceCapacity = maintenanceCapacity;
        this.fuelMax = fuelMax;
        this.weaponsMax = weaponsMax;
        this.sparePartsMax = sparePartsMax;
    }

    public Long getId() { return id; }
    public Game getGame() { return game; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public BaseType getBaseType() { return baseType; }
    public Integer getParkingCapacity() { return parkingCapacity; }
    public Integer getMaintenanceCapacity() { return maintenanceCapacity; }
    public Integer getFuelMax() { return fuelMax; }
    public Integer getWeaponsMax() { return weaponsMax; }
    public Integer getSparePartsMax() { return sparePartsMax; }
}
