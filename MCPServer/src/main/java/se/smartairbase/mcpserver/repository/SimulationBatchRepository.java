package se.smartairbase.mcpserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.smartairbase.mcpserver.domain.game.SimulationBatch;

public interface SimulationBatchRepository extends JpaRepository<SimulationBatch, Long> {
    boolean existsByNameIgnoreCase(String name);
}
