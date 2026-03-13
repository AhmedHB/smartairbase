package se.smartairbase.mcpserver.domain.rule;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scenario_base",
        uniqueConstraints = @UniqueConstraint(name = "uk_scenario_base_code", columnNames = {"scenario_id", "code"}))
/**
 * Scenario-level base definition including capacities, start stock, and delivery rules.
 */
public class ScenarioBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scenario_base_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

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

    @Column(name = "fuel_start", nullable = false)
    private Integer fuelStart;

    @Column(name = "fuel_max", nullable = false)
    private Integer fuelMax;

    @Column(name = "weapons_start", nullable = false)
    private Integer weaponsStart;

    @Column(name = "weapons_max", nullable = false)
    private Integer weaponsMax;

    @Column(name = "spare_parts_start", nullable = false)
    private Integer sparePartsStart;

    @Column(name = "spare_parts_max", nullable = false)
    private Integer sparePartsMax;

    @OneToMany(mappedBy = "scenarioBase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScenarioSupplyRule> supplyRules = new ArrayList<>();

    protected ScenarioBase() {
    }

    public ScenarioBase(
            Scenario scenario,
            String code,
            String name,
            BaseType baseType,
            Integer parkingCapacity,
            Integer maintenanceCapacity,
            Integer fuelStart,
            Integer fuelMax,
            Integer weaponsStart,
            Integer weaponsMax,
            Integer sparePartsStart,
            Integer sparePartsMax) {
        this.scenario = scenario;
        this.code = code;
        this.name = name;
        this.baseType = baseType;
        this.parkingCapacity = parkingCapacity;
        this.maintenanceCapacity = maintenanceCapacity;
        this.fuelStart = fuelStart;
        this.fuelMax = fuelMax;
        this.weaponsStart = weaponsStart;
        this.weaponsMax = weaponsMax;
        this.sparePartsStart = sparePartsStart;
        this.sparePartsMax = sparePartsMax;
    }

    public void updateCapacity(Integer parkingCapacity, Integer maintenanceCapacity) {
        this.parkingCapacity = parkingCapacity;
        this.maintenanceCapacity = maintenanceCapacity;
    }

    public void updateResources(Integer fuelStart,
                                Integer fuelMax,
                                Integer weaponsStart,
                                Integer weaponsMax,
                                Integer sparePartsStart,
                                Integer sparePartsMax) {
        this.fuelStart = fuelStart;
        this.fuelMax = fuelMax;
        this.weaponsStart = weaponsStart;
        this.weaponsMax = weaponsMax;
        this.sparePartsStart = sparePartsStart;
        this.sparePartsMax = sparePartsMax;
    }

    public Long getId() { return id; }
    public Scenario getScenario() { return scenario; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public BaseType getBaseType() { return baseType; }
    public Integer getParkingCapacity() { return parkingCapacity; }
    public Integer getMaintenanceCapacity() { return maintenanceCapacity; }
    public Integer getFuelStart() { return fuelStart; }
    public Integer getFuelMax() { return fuelMax; }
    public Integer getWeaponsStart() { return weaponsStart; }
    public Integer getWeaponsMax() { return weaponsMax; }
    public Integer getSparePartsStart() { return sparePartsStart; }
    public Integer getSparePartsMax() { return sparePartsMax; }
    public List<ScenarioSupplyRule> getSupplyRules() { return supplyRules; }
}
