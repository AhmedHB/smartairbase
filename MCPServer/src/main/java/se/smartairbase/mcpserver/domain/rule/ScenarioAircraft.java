package se.smartairbase.mcpserver.domain.rule;

import jakarta.persistence.*;

@Entity
@Table(name = "scenario_aircraft",
        uniqueConstraints = @UniqueConstraint(name = "uk_scenario_aircraft_code", columnNames = {"scenario_id", "code"}))
public class ScenarioAircraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scenario_aircraft_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aircraft_type_id", nullable = false)
    private AircraftType aircraftType;

    @Column(name = "start_base_code", nullable = false, length = 20)
    private String startBaseCode;

    @Column(name = "fuel_start", nullable = false)
    private Integer fuelStart;

    @Column(name = "weapons_start", nullable = false)
    private Integer weaponsStart;

    @Column(name = "flight_hours_start", nullable = false)
    private Integer flightHoursStart;

    protected ScenarioAircraft() {
    }

    public ScenarioAircraft(Scenario scenario,
                            String code,
                            AircraftType aircraftType,
                            String startBaseCode,
                            Integer fuelStart,
                            Integer weaponsStart,
                            Integer flightHoursStart) {
        this.scenario = scenario;
        this.code = code;
        this.aircraftType = aircraftType;
        this.startBaseCode = startBaseCode;
        this.fuelStart = fuelStart;
        this.weaponsStart = weaponsStart;
        this.flightHoursStart = flightHoursStart;
    }

    public void updateSetup(String startBaseCode, Integer fuelStart, Integer weaponsStart, Integer flightHoursStart) {
        this.startBaseCode = startBaseCode;
        this.fuelStart = fuelStart;
        this.weaponsStart = weaponsStart;
        this.flightHoursStart = flightHoursStart;
    }

    public Long getId() { return id; }
    public Scenario getScenario() { return scenario; }
    public String getCode() { return code; }
    public AircraftType getAircraftType() { return aircraftType; }
    public String getStartBaseCode() { return startBaseCode; }
    public Integer getFuelStart() { return fuelStart; }
    public Integer getWeaponsStart() { return weaponsStart; }
    public Integer getFlightHoursStart() { return flightHoursStart; }
}
