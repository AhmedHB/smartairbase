package se.smartairbase.mcpclient.domain;

public enum SmartAirBaseTool {
    CREATE_GAME("create_game"),
    GET_GAME_STATE("get_game_state"),
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
