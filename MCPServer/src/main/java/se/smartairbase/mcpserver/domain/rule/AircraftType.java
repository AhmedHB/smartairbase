package se.smartairbase.mcpserver.domain.rule;

import jakarta.persistence.*;

@Entity
@Table(name = "aircraft_type")
public class AircraftType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "aircraft_type_id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "max_fuel", nullable = false)
    private Integer maxFuel;

    @Column(name = "max_weapons", nullable = false)
    private Integer maxWeapons;

    @Column(name = "max_flight_hours", nullable = false)
    private Integer maxFlightHours;

    protected AircraftType() {
    }

    public AircraftType(String code, String name, Integer maxFuel, Integer maxWeapons, Integer maxFlightHours) {
        this.code = code;
        this.name = name;
        this.maxFuel = maxFuel;
        this.maxWeapons = maxWeapons;
        this.maxFlightHours = maxFlightHours;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public Integer getMaxFuel() { return maxFuel; }
    public Integer getMaxWeapons() { return maxWeapons; }
    public Integer getMaxFlightHours() { return maxFlightHours; }
}
