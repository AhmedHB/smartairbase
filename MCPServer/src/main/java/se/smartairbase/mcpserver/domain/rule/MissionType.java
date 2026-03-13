package se.smartairbase.mcpserver.domain.rule;

import jakarta.persistence.*;

@Entity
@Table(name = "mission_type")
/**
 * Seeded mission template that defines standard mission costs and labels.
 */
public class MissionType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mission_type_id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "flight_time_cost", nullable = false)
    private Integer flightTimeCost;

    @Column(name = "fuel_cost", nullable = false)
    private Integer fuelCost;

    @Column(name = "weapon_cost", nullable = false)
    private Integer weaponCost;

    protected MissionType() {
    }

    public MissionType(String code, String name, Integer flightTimeCost, Integer fuelCost, Integer weaponCost) {
        this.code = code;
        this.name = name;
        this.flightTimeCost = flightTimeCost;
        this.fuelCost = fuelCost;
        this.weaponCost = weaponCost;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public Integer getFlightTimeCost() { return flightTimeCost; }
    public Integer getFuelCost() { return fuelCost; }
    public Integer getWeaponCost() { return weaponCost; }
}
