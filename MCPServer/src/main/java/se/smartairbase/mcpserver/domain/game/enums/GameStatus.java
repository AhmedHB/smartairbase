package se.smartairbase.mcpserver.domain.game.enums;

/**
 * High-level lifecycle state for one game instance.
 */
public enum GameStatus {
    NOT_STARTED,
    ACTIVE,
    ABORTED,
    WON,
    LOST
}
