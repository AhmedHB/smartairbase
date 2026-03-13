package se.smartairbase.mcpserver.config;

import io.modelcontextprotocol.server.McpServerFeatures;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.smartairbase.mcpserver.mcp.tools.AircraftTools;
import se.smartairbase.mcpserver.mcp.tools.BaseTools;
import se.smartairbase.mcpserver.mcp.tools.GameTools;
import se.smartairbase.mcpserver.mcp.tools.MissionTools;
import se.smartairbase.mcpserver.mcp.tools.RoundTools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Configuration
/**
 * Collects tool beans and exposes them as MCP tool specifications for Spring AI.
 */
public class ToolConfig {

    @Bean
    public List<McpServerFeatures.SyncToolSpecification> tools(
            GameTools gameTools,
            RoundTools roundTools,
            MissionTools missionTools,
            AircraftTools aircraftTools,
            BaseTools baseTools
    ) {
        List<ToolCallback> callbacks = new ArrayList<>();

        Stream.of(gameTools, roundTools, missionTools, aircraftTools, baseTools)
                .forEach(toolBean -> callbacks.addAll(Arrays.asList(
                        MethodToolCallbackProvider.builder()
                                .toolObjects(toolBean)
                                .build()
                                .getToolCallbacks()
                )));

        callbacks.sort(Comparator.comparing(cb -> cb.getToolDefinition().name()));
        callbacks.forEach(cb -> System.out.println("TOOL: " + cb.getToolDefinition().name()));

        return McpToolUtils.toSyncToolSpecifications(callbacks.toArray(new ToolCallback[0]));
    }
}
