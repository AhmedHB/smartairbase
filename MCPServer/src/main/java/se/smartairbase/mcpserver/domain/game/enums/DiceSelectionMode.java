package se.smartairbase.mcpserver.domain.game.enums;

/**
 * Exact method used to choose one persisted dice roll.
 */
public enum DiceSelectionMode {
    MANUAL_DIRECT_SELECTION,
    MANUAL_RANDOM_SELECTION,
    AUTO_RANDOM,
    AUTO_MIN_DAMAGE,
    AUTO_MAX_DAMAGE
}
