package se.smartairbase.mcpserver.domain.game;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "game_analysis_entry",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_game_analysis_entry_game_round_role",
                        columnNames = {"game_id", "round_number", "role_name"}
                )
        }
)
/**
 * Persisted analysis-feed entry attached to one game, round, and narrative role.
 */
public class GameAnalysisEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_analysis_entry_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @Column(name = "phase", length = 40)
    private String phase;

    @Column(name = "role_name", nullable = false, length = 120)
    private String roleName;

    @Column(name = "source", nullable = false, length = 40)
    private String source;

    @Column(name = "summary", nullable = false, length = 1000)
    private String summary;

    @Column(name = "details", length = 2000)
    private String details;

    @Column(name = "related_aircraft", length = 1000)
    private String relatedAircraft;

    @Column(name = "related_bases", length = 1000)
    private String relatedBases;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected GameAnalysisEntry() {
    }

    public GameAnalysisEntry(Game game,
                             Integer roundNumber,
                             String phase,
                             String roleName,
                             String source,
                             String summary,
                             String details,
                             String relatedAircraft,
                             String relatedBases,
                             LocalDateTime createdAt) {
        this.game = game;
        this.roundNumber = roundNumber;
        this.phase = phase;
        this.roleName = roleName;
        this.source = source;
        this.summary = summary;
        this.details = details;
        this.relatedAircraft = relatedAircraft;
        this.relatedBases = relatedBases;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Game getGame() {
        return game;
    }

    public Integer getRoundNumber() {
        return roundNumber;
    }

    public String getPhase() {
        return phase;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getSource() {
        return source;
    }

    public String getSummary() {
        return summary;
    }

    public String getDetails() {
        return details;
    }

    public String getRelatedAircraft() {
        return relatedAircraft;
    }

    public String getRelatedBases() {
        return relatedBases;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
