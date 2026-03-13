package se.smartairbase.mcpserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.smartairbase.mcpserver.domain.game.SimulationBatchGame;

import java.util.List;

/**
 * Persists the game runs that belong to a simulation batch.
 */
public interface SimulationBatchGameRepository extends JpaRepository<SimulationBatchGame, Long> {
    List<SimulationBatchGame> findBySimulationBatch_IdOrderByRunNumberAsc(Long simulationBatchId);
    List<SimulationBatchGame> findBySimulationBatch_IdOrderByRunNumberDesc(Long simulationBatchId);
}
