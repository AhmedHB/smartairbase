package se.smartairbase.mcpserver.domain.game;

import jakarta.persistence.*;
import se.smartairbase.mcpserver.domain.game.enums.AircraftStatus;
import se.smartairbase.mcpserver.domain.rule.AircraftType;

@Entity
@Table(name = "game_aircraft", uniqueConstraints = @UniqueConstraint(name = "uk_game_aircraft_code", columnNames = {"game_id", "code"}))
public class GameAircraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_aircraft_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aircraft_type_id", nullable = false)
    private AircraftType aircraftType;

    @Column(name = "fuel_capacity", nullable = false)
    private Integer fuelCapacity;

    @Column(name = "weapons_capacity", nullable = false)
    private Integer weaponsCapacity;

    @Column(name = "flight_hours_capacity", nullable = false)
    private Integer flightHoursCapacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AircraftStatus status = AircraftStatus.READY;

    protected GameAircraft() {
    }

    public GameAircraft(Game game,
                        String code,
                        AircraftType aircraftType,
                        Integer fuelCapacity,
                        Integer weaponsCapacity,
                        Integer flightHoursCapacity) {
        this.game = game;
        this.code = code;
        this.aircraftType = aircraftType;
        this.fuelCapacity = fuelCapacity;
        this.weaponsCapacity = weaponsCapacity;
        this.flightHoursCapacity = flightHoursCapacity;
    }

    public void setStatus(AircraftStatus status) {
        this.status = status;
    }

    public Long getId() { return id; }
    public Game getGame() { return game; }
    public String getCode() { return code; }
    public AircraftType getAircraftType() { return aircraftType; }
    public Integer getFuelCapacity() { return fuelCapacity; }
    public Integer getWeaponsCapacity() { return weaponsCapacity; }
    public Integer getFlightHoursCapacity() { return flightHoursCapacity; }
    public AircraftStatus getStatus() { return status; }
}
