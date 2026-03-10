package se.smartairbase.mcpserver.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import se.smartairbase.mcpserver.service.RoundService;

@Component
public class RoundTools {

    private final RoundService roundService;

    public RoundTools(RoundService roundService) {
        this.roundService = roundService;
    }

    @Tool(
            name = "start_round",
            description = "Start a new round if the game is active and no round is already open"
    )
    public Object startRound(Long gameId) {
        return roundService.startRound(gameId);
    }

    @Tool(
            name = "resolve_missions",
            description = "Resolve all assigned missions in the current planning phase"
    )
    public Object resolveMissions(Long gameId) {
        return roundService.resolveMissions(gameId);
    }

    @Tool(
            name = "record_dice_roll",
            description = "Record a player-provided dice roll for an aircraft awaiting damage resolution"
    )
    public Object recordDiceRoll(Long gameId, String aircraftCode, Integer diceValue) {
        return roundService.recordDiceRoll(gameId, aircraftCode, diceValue);
    }

    @Tool(
            name = "list_available_landing_bases",
            description = "List landing bases available for an aircraft after its dice roll is resolved"
    )
    public Object listAvailableLandingBases(Long gameId, String aircraftCode) {
        return roundService.listAvailableLandingBases(gameId, aircraftCode);
    }

    @Tool(
            name = "land_aircraft",
            description = "Land an aircraft at a selected base during the landing phase"
    )
    public Object landAircraft(Long gameId, String aircraftCode, String baseCode) {
        return roundService.landAircraft(gameId, aircraftCode, baseCode);
    }

    @Tool(
            name = "send_aircraft_to_holding",
            description = "Send an aircraft to holding when no base can accept it"
    )
    public Object sendAircraftToHolding(Long gameId, String aircraftCode) {
        return roundService.sendAircraftToHolding(gameId, aircraftCode);
    }

    @Tool(
            name = "complete_round",
            description = "Complete the active round after all dice rolls and landings are resolved"
    )
    public Object completeRound(Long gameId) {
        return roundService.completeRound(gameId);
    }
}
