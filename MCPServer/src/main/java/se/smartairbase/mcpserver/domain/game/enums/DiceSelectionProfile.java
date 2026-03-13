package se.smartairbase.mcpserver.domain.game.enums;

/**
 * Derived game-level summary of the dice selection styles used across all rolls.
 */
public enum DiceSelectionProfile {
    MANUAL_DIRECT_SELECTION,
    MANUAL_RANDOM_SELECTION,
    MANUAL_MIXED,
    AUTO_RANDOM,
    AUTO_MIN_DAMAGE,
    AUTO_MAX_DAMAGE,
    MIXED
}
