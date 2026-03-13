package se.smartairbase.mcpserver.domain.game.enums;

/**
 * Lifecycle states that a game aircraft can occupy during play.
 */
public enum AircraftStatus {
    READY,
    ON_MISSION,
    AWAITING_DICE_ROLL,
    AWAITING_LANDING,
    PARKED,
    WAITING_MAINTENANCE,
    IN_MAINTENANCE,
    HOLDING,
    CRASHED,
    DESTROYED
}
