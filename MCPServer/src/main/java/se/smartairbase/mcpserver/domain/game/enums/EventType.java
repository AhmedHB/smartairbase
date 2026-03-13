package se.smartairbase.mcpserver.domain.game.enums;

/**
 * Event categories written to the game event log.
 */
public enum EventType {
    MISSION_ASSIGNED,
    TAKEOFF,
    MISSION_COMPLETED,
    DICE_ROLLED,
    LANDING,
    ENTER_HOLDING,
    EXIT_HOLDING,
    REFUEL,
    REARM,
    REPAIR_START,
    REPAIR_PROGRESS,
    REPAIR_COMPLETE,
    FULL_SERVICE_START,
    FULL_SERVICE_COMPLETE,
    SUPPLY_DELIVERY,
    CRASH
}
