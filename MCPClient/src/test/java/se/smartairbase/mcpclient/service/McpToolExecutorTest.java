package se.smartairbase.mcpclient.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import se.smartairbase.mcpclient.domain.SmartAirBaseTool;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpToolExecutorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void resolvesToolBySuffix() {
        McpToolExecutor executor = new McpToolExecutor(
                List.of(provider(new FakeToolCallback("intelliplan_create_game", "{\"gameId\":\"g-1\"}"))),
                objectMapper
        );

        String gameId = executor.execute(SmartAirBaseTool.CREATE_GAME, Map.of("scenarioName", "smartairbase")).get("gameId").asText();

        assertThat(gameId).isEqualTo("g-1");
    }

    @Test
    void failsWhenMultipleToolsShareSameSuffix() {
        McpToolExecutor executor = new McpToolExecutor(
                List.of(provider(
                        new FakeToolCallback("alpha_create_game", "{}"),
                        new FakeToolCallback("beta_create_game", "{}"))),
                objectMapper
        );

        assertThatThrownBy(() -> executor.resolveToolCallback(SmartAirBaseTool.CREATE_GAME))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Multiple MCP tools matched suffix");
    }

    private ToolCallbackProvider provider(ToolCallback... callbacks) {
        return () -> callbacks;
    }

    private record FakeToolCallback(String name, String response) implements ToolCallback {

        @Override
        public ToolDefinition getToolDefinition() {
            return DefaultToolDefinition.builder()
                    .name(name)
                    .description("test")
                    .inputSchema("{}")
                    .build();
        }

        @Override
        public String call(String toolInput) {
            return response;
        }

        @Override
        public String call(String toolInput, ToolContext toolContext) {
            return response;
        }
    }
}
