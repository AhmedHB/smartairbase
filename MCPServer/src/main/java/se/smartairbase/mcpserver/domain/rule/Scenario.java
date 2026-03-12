package se.smartairbase.mcpserver.domain.rule;

import jakarta.persistence.*;
import se.smartairbase.mcpserver.domain.rule.enums.ScenarioSourceType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scenario")
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scenario_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "version", nullable = false, length = 20)
    private String version;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private ScenarioSourceType sourceType = ScenarioSourceType.SYSTEM;

    @Column(name = "editable", nullable = false)
    private boolean editable;

    @Column(name = "deletable", nullable = false)
    private boolean deletable;

    @Column(name = "published", nullable = false)
    private boolean published = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScenarioBase> bases = new ArrayList<>();

    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScenarioAircraft> aircraft = new ArrayList<>();

    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScenarioMission> missions = new ArrayList<>();

    protected Scenario() {
    }

    public Scenario(String name, String version, String description, LocalDateTime createdAt) {
        this.name = name;
        this.version = version;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public Scenario(String name,
                    String version,
                    String description,
                    ScenarioSourceType sourceType,
                    boolean editable,
                    boolean deletable,
                    boolean published,
                    LocalDateTime createdAt,
                    LocalDateTime updatedAt) {
        this.name = name;
        this.version = version;
        this.description = description;
        this.sourceType = sourceType;
        this.editable = editable;
        this.deletable = deletable;
        this.published = published;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getDescription() { return description; }
    public ScenarioSourceType getSourceType() { return sourceType; }
    public boolean isEditable() { return editable; }
    public boolean isDeletable() { return deletable; }
    public boolean isPublished() { return published; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<ScenarioBase> getBases() { return bases; }
    public List<ScenarioAircraft> getAircraft() { return aircraft; }
    public List<ScenarioMission> getMissions() { return missions; }

    public void updateDescription(String description, LocalDateTime updatedAt) {
        this.description = description;
        this.updatedAt = updatedAt;
    }
}
