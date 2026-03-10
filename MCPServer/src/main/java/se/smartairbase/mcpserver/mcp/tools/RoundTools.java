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
            name = "execute_round",
            description = "Execute one full round of the game"
    )
    public Object executeRound(Long gameId) {
        return roundService.executeRound(gameId);
    }
}