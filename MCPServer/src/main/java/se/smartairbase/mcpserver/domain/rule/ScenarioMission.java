package se.smartairbase.mcpserver.domain.rule;

import jakarta.persistence.*;

@Entity
@Table(name = "scenario_mission",
        uniqueConstraints = @UniqueConstraint(name = "uk_scenario_mission_code", columnNames = {"scenario_id", "code"}))
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

    protected ScenarioMission() {
    }

    public ScenarioMission(Scenario scenario, String code, MissionType missionType, Integer sortOrder) {
        this.scenario = scenario;
        this.code = code;
        this.missionType = missionType;
        this.sortOrder = sortOrder;
    }

    public Long getId() { return id; }
    public Scenario getScenario() { return scenario; }
    public String getCode() { return code; }
    public MissionType getMissionType() { return missionType; }
    public Integer getSortOrder() { return sortOrder; }
}
