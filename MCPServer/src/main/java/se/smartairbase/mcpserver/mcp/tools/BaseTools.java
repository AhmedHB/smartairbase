package se.smartairbase.mcpserver.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import se.smartairbase.mcpserver.service.BaseService;

@Component
/**
 * Exposes base lookup operations as MCP tools.
 */
public class BaseTools {

    private final BaseService baseService;

    public BaseTools(BaseService baseService) {
        this.baseService = baseService;
    }

    @Tool(
            name = "get_base_state",
            description = "Get the state of a base"
    )
    public Object getBaseState(Long gameId, String baseCode) {
        return baseService.getBaseState(gameId, baseCode);
    }
}
