package se.smartairbase.mcpserver.domain.rule;

import jakarta.persistence.*;

@Entity
@Table(name = "scenario_mission",
        uniqueConstraints = @UniqueConstraint(name = "uk_scenario_mission_code", columnNames = {"scenario_id", "code"}))
/**
 * Scenario mission template that controls mission mix and per-mission costs.
 */
public class ScenarioMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scenario_mission_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mission_type_id", nullable = false)
    private MissionType missionType;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "default_count", nullable = false)
    private Integer defaultCount;

    @Column(name = "fuel_cost", nullable = false)
    private Integer fuelCost;

    @Column(name = "weapon_cost", nullable = false)
    private Integer weaponCost;

    @Column(name = "flight_time_cost", nullable = false)
    private Integer flightTimeCost;

    protected ScenarioMission() {
    }

    public ScenarioMission(Scenario scenario,
                           String code,
                           MissionType missionType,
                           Integer sortOrder,
                           Integer defaultCount,
                           Integer fuelCost,
                           Integer weaponCost,
                           Integer flightTimeCost) {
        this.scenario = scenario;
        this.code = code;
        this.missionType = missionType;
        this.sortOrder = sortOrder;
        this.defaultCount = defaultCount;
        this.fuelCost = fuelCost;
        this.weaponCost = weaponCost;
        this.flightTimeCost = flightTimeCost;
    }

    public void updateSetup(Integer fuelCost, Integer weaponCost, Integer flightTimeCost) {
        this.fuelCost = fuelCost;
        this.weaponCost = weaponCost;
        this.flightTimeCost = flightTimeCost;
    }

    public Long getId() { return id; }
    public Scenario getScenario() { return scenario; }
    public String getCode() { return code; }
    public MissionType getMissionType() { return missionType; }
    public Integer getSortOrder() { return sortOrder; }
    public Integer getDefaultCount() { return defaultCount; }
    public Integer getFuelCost() { return fuelCost; }
    public Integer getWeaponCost() { return weaponCost; }
    public Integer getFlightTimeCost() { return flightTimeCost; }
}
