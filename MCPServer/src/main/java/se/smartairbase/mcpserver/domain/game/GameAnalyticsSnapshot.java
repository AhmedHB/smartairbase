package se.smartairbase.mcpserver.domain.game;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Stores one analytics row per finished game so completed runs can be analyzed as a stable dataset.
 */
@Entity
@Table(name = "game_analytics_snapshot")
public class GameAnalyticsSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "game_analytics_snapshot_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false, unique = true)
    private Game game;

    @Column(name = "scenario_name", nullable = false, length = 100)
    private String scenarioName;

    @Column(name = "game_status", nullable = false, length = 20)
    private String gameStatus;

    @Column(name = "is_win", nullable = false)
    private boolean win;

    @Column(name = "rounds_to_outcome", nullable = false)
    private Integer roundsToOutcome;

    @Column(name = "dice_selection_profile", length = 40)
    private String diceSelectionProfile;

    @Column(name = "aircraft_count", nullable = false)
    private Integer aircraftCount;

    @Column(name = "surviving_aircraft_count", nullable = false)
    private Integer survivingAircraftCount;

    @Column(name = "destroyed_aircraft_count", nullable = false)
    private Integer destroyedAircraftCount;

    @Column(name = "mission_count", nullable = false)
    private Integer missionCount;

    @Column(name = "completed_mission_count", nullable = false)
    private Integer completedMissionCount;

    @Column(name = "m1_count", nullable = false)
    private Integer m1Count;

    @Column(name = "m2_count", nullable = false)
    private Integer m2Count;

    @Column(name = "m3_count", nullable = false)
    private Integer m3Count;

    @Column(name = "total_parking_capacity", nullable = false)
    private Integer totalParkingCapacity;

    @Column(name = "total_maintenance_capacity", nullable = false)
    private Integer totalMaintenanceCapacity;

    @Column(name = "total_fuel_start", nullable = false)
    private Integer totalFuelStart;

    @Column(name = "total_fuel_max", nullable = false)
    private Integer totalFuelMax;

    @Column(name = "total_weapons_start", nullable = false)
    private Integer totalWeaponsStart;

    @Column(name = "total_weapons_max", nullable = false)
    private Integer totalWeaponsMax;

    @Column(name = "total_spare_parts_start", nullable = false)
    private Integer totalSparePartsStart;

    @Column(name = "total_spare_parts_max", nullable = false)
    private Integer totalSparePartsMax;

    @Column(name = "fuel_delivery_amount_total", nullable = false)
    private Integer fuelDeliveryAmountTotal;

    @Column(name = "weapons_delivery_amount_total", nullable = false)
    private Integer weaponsDeliveryAmountTotal;

    @Column(name = "spare_parts_delivery_amount_total", nullable = false)
    private Integer sparePartsDeliveryAmountTotal;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected GameAnalyticsSnapshot() {
    }

    public GameAnalyticsSnapshot(Game game,
                                 String scenarioName,
                                 String gameStatus,
                                 boolean win,
                                 Integer roundsToOutcome,
                                 String diceSelectionProfile,
                                 Integer aircraftCount,
                                 Integer survivingAircraftCount,
                                 Integer destroyedAircraftCount,
                                 Integer missionCount,
                                 Integer completedMissionCount,
                                 Integer m1Count,
                                 Integer m2Count,
                                 Integer m3Count,
                                 Integer totalParkingCapacity,
                                 Integer totalMaintenanceCapacity,
                                 Integer totalFuelStart,
                                 Integer totalFuelMax,
                                 Integer totalWeaponsStart,
                                 Integer totalWeaponsMax,
                                 Integer totalSparePartsStart,
                                 Integer totalSparePartsMax,
                                 Integer fuelDeliveryAmountTotal,
                                 Integer weaponsDeliveryAmountTotal,
                                 Integer sparePartsDeliveryAmountTotal,
                                 LocalDateTime createdAt) {
        this.game = game;
        this.scenarioName = scenarioName;
        this.gameStatus = gameStatus;
        this.win = win;
        this.roundsToOutcome = roundsToOutcome;
        this.diceSelectionProfile = diceSelectionProfile;
        this.aircraftCount = aircraftCount;
        this.survivingAircraftCount = survivingAircraftCount;
        this.destroyedAircraftCount = destroyedAircraftCount;
        this.missionCount = missionCount;
        this.completedMissionCount = completedMissionCount;
        this.m1Count = m1Count;
        this.m2Count = m2Count;
        this.m3Count = m3Count;
        this.totalParkingCapacity = totalParkingCapacity;
        this.totalMaintenanceCapacity = totalMaintenanceCapacity;
        this.totalFuelStart = totalFuelStart;
        this.totalFuelMax = totalFuelMax;
        this.totalWeaponsStart = totalWeaponsStart;
        this.totalWeaponsMax = totalWeaponsMax;
        this.totalSparePartsStart = totalSparePartsStart;
        this.totalSparePartsMax = totalSparePartsMax;
        this.fuelDeliveryAmountTotal = fuelDeliveryAmountTotal;
        this.weaponsDeliveryAmountTotal = weaponsDeliveryAmountTotal;
        this.sparePartsDeliveryAmountTotal = sparePartsDeliveryAmountTotal;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Game getGame() { return game; }
    public String getScenarioName() { return scenarioName; }
    public String getGameStatus() { return gameStatus; }
    public boolean isWin() { return win; }
    public Integer getRoundsToOutcome() { return roundsToOutcome; }
    public String getDiceSelectionProfile() { return diceSelectionProfile; }
    public Integer getAircraftCount() { return aircraftCount; }
    public Integer getSurvivingAircraftCount() { return survivingAircraftCount; }
    public Integer getDestroyedAircraftCount() { return destroyedAircraftCount; }
    public Integer getMissionCount() { return missionCount; }
    public Integer getCompletedMissionCount() { return completedMissionCount; }
    public Integer getM1Count() { return m1Count; }
    public Integer getM2Count() { return m2Count; }
    public Integer getM3Count() { return m3Count; }
    public Integer getTotalParkingCapacity() { return totalParkingCapacity; }
    public Integer getTotalMaintenanceCapacity() { return totalMaintenanceCapacity; }
    public Integer getTotalFuelStart() { return totalFuelStart; }
    public Integer getTotalFuelMax() { return totalFuelMax; }
    public Integer getTotalWeaponsStart() { return totalWeaponsStart; }
    public Integer getTotalWeaponsMax() { return totalWeaponsMax; }
    public Integer getTotalSparePartsStart() { return totalSparePartsStart; }
    public Integer getTotalSparePartsMax() { return totalSparePartsMax; }
    public Integer getFuelDeliveryAmountTotal() { return fuelDeliveryAmountTotal; }
    public Integer getWeaponsDeliveryAmountTotal() { return weaponsDeliveryAmountTotal; }
    public Integer getSparePartsDeliveryAmountTotal() { return sparePartsDeliveryAmountTotal; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
