package se.smartairbase.mcpserver.domain.game;

import jakarta.persistence.*;
import se.smartairbase.mcpserver.domain.game.enums.MissionStatus;
import se.smartairbase.mcpserver.domain.rule.MissionType;

@Entity
@Table(name = "game_mission", uniqueConstraints = @UniqueConstraint(name = "uk_game_mission_code", columnNames = {"game_id", "code"}))
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MissionStatus status = MissionStatus.AVAILABLE;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    protected GameMission() {
    }

    public GameMission(Game game, String code, MissionType missionType, Integer sortOrder) {
        this.game = game;
        this.code = code;
        this.missionType = missionType;
        this.sortOrder = sortOrder;
    }

    public void setStatus(MissionStatus status) {
        this.status = status;
    }

    public Long getId() { return id; }
    public Game getGame() { return game; }
    public String getCode() { return code; }
    public MissionType getMissionType() { return missionType; }
    public MissionStatus getStatus() { return status; }
    public Integer getSortOrder() { return sortOrder; }
}
