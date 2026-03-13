package se.smartairbase.mcpserver.domain.game.enums;

/**
 * Execution state for one game inside a simulator batch.
 */
public enum SimulationBatchGameStatus {
    CREATED,
    RUNNING,
    COMPLETED,
    FAILED
}
