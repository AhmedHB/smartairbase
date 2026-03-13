package se.smartairbase.mcpserver.domain.game;

import jakarta.persistence.*;
import se.smartairbase.mcpserver.domain.game.enums.MissionStatus;
import se.smartairbase.mcpserver.domain.rule.MissionType;

@Entity
@Table(name = "game_mission", uniqueConstraints = @UniqueConstraint(name = "uk_game_mission_code", columnNames = {"game_id", "code"}))
/**
 * One mission instance that belongs to a live game.
 */
public class GameMission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_mission_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mission_type_id", nullable = false)
    private MissionType missionType;

    @Column(name = "fuel_cost", nullable = false)
    private Integer fuelCost;

    @Column(name = "weapon_cost", nullable = false)
    private Integer weaponCost;

    @Column(name = "flight_time_cost", nullable = false)
    private Integer flightTimeCost;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MissionStatus status = MissionStatus.AVAILABLE;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    protected GameMission() {
    }

    public GameMission(Game game,
                       String code,
                       MissionType missionType,
                       Integer sortOrder,
                       Integer fuelCost,
                       Integer weaponCost,
                       Integer flightTimeCost) {
        this.game = game;
        this.code = code;
        this.missionType = missionType;
        this.sortOrder = sortOrder;
        this.fuelCost = fuelCost;
        this.weaponCost = weaponCost;
        this.flightTimeCost = flightTimeCost;
    }

    public void setStatus(MissionStatus status) {
        this.status = status;
    }

    public Long getId() { return id; }
    public Game getGame() { return game; }
    public String getCode() { return code; }
    public MissionType getMissionType() { return missionType; }
    public Integer getFuelCost() { return fuelCost; }
    public Integer getWeaponCost() { return weaponCost; }
    public Integer getFlightTimeCost() { return flightTimeCost; }
    public MissionStatus getStatus() { return status; }
    public Integer getSortOrder() { return sortOrder; }
}
