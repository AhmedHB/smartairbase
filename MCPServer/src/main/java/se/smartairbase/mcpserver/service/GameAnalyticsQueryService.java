package se.smartairbase.mcpserver.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.smartairbase.mcpserver.domain.game.GameAnalyticsSnapshot;
import se.smartairbase.mcpserver.mcp.dto.GameAnalyticsSnapshotDto;
import se.smartairbase.mcpserver.repository.GameAnalyticsSnapshotRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Exposes filtered read access to the finished-game analytics dataset.
 */
@Service
public class GameAnalyticsQueryService {

    private final GameAnalyticsSnapshotRepository gameAnalyticsSnapshotRepository;

    public GameAnalyticsQueryService(GameAnalyticsSnapshotRepository gameAnalyticsSnapshotRepository) {
        this.gameAnalyticsSnapshotRepository = gameAnalyticsSnapshotRepository;
    }

    @Transactional(readOnly = true)
    public GameAnalyticsSnapshotDto getSnapshotByGameId(Long gameId) {
        return gameAnalyticsSnapshotRepository.findByGame_Id(gameId)
                .map(this::toDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<GameAnalyticsSnapshotDto> listSnapshots(String scenarioName,
                                                        String createdDate,
                                                        Integer aircraftCount,
                                                        Integer m1Count,
                                                        Integer m2Count,
                                                        Integer m3Count) {
        LocalDate filterDate = parseDate(createdDate);
        return gameAnalyticsSnapshotRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(snapshot -> scenarioName == null || scenarioName.isBlank() || snapshot.getScenarioName().equalsIgnoreCase(scenarioName.trim()))
                .filter(snapshot -> filterDate == null || snapshot.getCreatedAt().toLocalDate().isEqual(filterDate))
                .filter(snapshot -> aircraftCount == null || snapshot.getAircraftCount().equals(aircraftCount))
                .filter(snapshot -> m1Count == null || snapshot.getM1Count().equals(m1Count))
                .filter(snapshot -> m2Count == null || snapshot.getM2Count().equals(m2Count))
                .filter(snapshot -> m3Count == null || snapshot.getM3Count().equals(m3Count))
                .map(this::toDto)
                .toList();
    }

    private GameAnalyticsSnapshotDto toDto(GameAnalyticsSnapshot snapshot) {
        return new GameAnalyticsSnapshotDto(
                snapshot.getId(),
                snapshot.getGame().getId(),
                snapshot.getGame().getName(),
                snapshot.getScenarioName(),
                snapshot.getGameStatus(),
                snapshot.isWin(),
                snapshot.getRoundsToOutcome(),
                snapshot.getDiceSelectionProfile(),
                snapshot.getAircraftCount(),
                snapshot.getSurvivingAircraftCount(),
                snapshot.getDestroyedAircraftCount(),
                snapshot.getMissionCount(),
                snapshot.getCompletedMissionCount(),
                snapshot.getM1Count(),
                snapshot.getM2Count(),
                snapshot.getM3Count(),
                snapshot.getCreatedAt() != null ? snapshot.getCreatedAt().toString() : null
        );
    }

    private LocalDate parseDate(String createdDate) {
        if (createdDate == null || createdDate.isBlank()) {
            return null;
        }
        return LocalDate.parse(createdDate);
    }
}
