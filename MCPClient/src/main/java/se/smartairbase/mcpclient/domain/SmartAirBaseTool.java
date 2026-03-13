package se.smartairbase.mcpclient.domain;

/**
 * Enumerates MCP tool names used by the client facade.
 */
public enum SmartAirBaseTool {
    CREATE_GAME("create_game"),
    CREATE_GAME_FROM_SCENARIO("create_game_from_scenario"),
    CREATE_SIMULATION_BATCH("create_simulation_batch"),
    GET_SIMULATION_BATCH("get_simulation_batch"),
    LIST_GAME_ANALYTICS_SNAPSHOTS("list_game_analytics_snapshots"),
    GET_GAME_STATE("get_game_state"),
    LIST_SCENARIOS("list_scenarios"),
    GET_SCENARIO("get_scenario"),
    DUPLICATE_SCENARIO("duplicate_scenario"),
    UPDATE_SCENARIO("update_scenario"),
    DELETE_SCENARIO("delete_scenario"),
    ABORT_GAME("abort_game"),
    LIST_ANALYSIS_FEED("list_analysis_feed"),
    APPEND_ANALYSIS_FEED_ITEMS("append_analysis_feed_items"),
    ASSIGN_MISSION("assign_mission"),
    START_ROUND("start_round"),
    RESOLVE_MISSIONS("resolve_missions"),
    RECORD_DICE_ROLL("record_dice_roll"),
    LIST_AVAILABLE_LANDING_BASES("list_available_landing_bases"),
    LAND_AIRCRAFT("land_aircraft"),
    SEND_AIRCRAFT_TO_HOLDING("send_aircraft_to_holding"),
    COMPLETE_ROUND("complete_round"),
    GET_AIRCRAFT_STATE("get_aircraft_state"),
    GET_BASE_STATE("get_base_state");

    private final String suffix;

    SmartAirBaseTool(String suffix) {
        this.suffix = suffix;
    }

    public String suffix() {
        return suffix;
    }
}
