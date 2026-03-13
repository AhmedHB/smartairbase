package se.smartairbase.mcpserver.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import se.smartairbase.mcpserver.service.AircraftService;

@Component
/**
 * Exposes aircraft lookup operations as MCP tools.
 */
public class AircraftTools {

    private final AircraftService aircraftService;

    public AircraftTools(AircraftService aircraftService) {
        this.aircraftService = aircraftService;
    }

    @Tool(
            name = "get_aircraft_state",
            description = "Get state of a specific aircraft"
    )
    public Object getAircraftState(Long gameId, String aircraftCode) {
        return aircraftService.getAircraftState(gameId, aircraftCode);
    }
}
