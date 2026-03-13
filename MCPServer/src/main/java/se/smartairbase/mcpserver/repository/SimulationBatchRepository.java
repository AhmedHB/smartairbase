package se.smartairbase.mcpserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.smartairbase.mcpserver.domain.game.SimulationBatch;

/**
 * Persists top-level simulation batch records.
 */
public interface SimulationBatchRepository extends JpaRepository<SimulationBatch, Long> {
    boolean existsByNameIgnoreCase(String name);
}
