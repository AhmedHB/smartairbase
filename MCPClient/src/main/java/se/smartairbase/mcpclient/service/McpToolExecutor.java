package se.smartairbase.mcpclient.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;
import se.smartairbase.mcpclient.domain.SmartAirBaseTool;

import java.util.ArrayList;
import java.util.List;

@Service
/**
 * Thin execution layer for MCP tools discovered through Spring AI.
 *
 * <p>The client code addresses tools through {@link SmartAirBaseTool} rather than
 * raw callback names. This service resolves the matching callback, serializes the
 * request payload and parses the JSON response into a tree.</p>
 */
public class McpToolExecutor {

    private final List<ToolCallbackProvider> toolCallbackProviders;
    private final ObjectMapper objectMapper;

    public McpToolExecutor(List<ToolCallbackProvider> toolCallbackProviders, ObjectMapper objectMapper) {
        this.toolCallbackProviders = toolCallbackProviders;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes one MCP tool call and returns the raw JSON response.
     */
    public <T> T execute(SmartAirBaseTool tool, Object request, Class<T> responseType) {
        ToolCallback callback = resolveToolCallback(tool);
        try {
            String payload = objectMapper.writeValueAsString(request);
            String response = callback.call(payload);
            JsonNode unwrapped = unwrapToolResponse(response);
            return objectMapper.treeToValue(unwrapped, responseType);
        }
        catch (Exception exception) {
            throw new IllegalStateException("Failed to execute MCP tool " + tool.suffix(), exception);
        }
    }

    private JsonNode unwrapToolResponse(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        if (root.isArray() && !root.isEmpty()) {
            JsonNode first = root.get(0);
            JsonNode text = first.get("text");
            if (text != null && text.isTextual()) {
                String textValue = text.textValue();
                try {
                    return objectMapper.readTree(textValue);
                }
                catch (Exception ignored) {
                    return objectMapper.getNodeFactory().textNode(textValue);
                }
            }
        }
        return root;
    }

    /**
     * Finds exactly one registered tool callback for the given logical tool.
     */
    ToolCallback resolveToolCallback(SmartAirBaseTool tool) {
        List<ToolCallback> matches = new ArrayList<>();
        for (ToolCallbackProvider provider : toolCallbackProviders) {
            for (ToolCallback callback : provider.getToolCallbacks()) {
                String name = callback.getToolDefinition().name();
                if (name.equals(tool.suffix()) || name.endsWith(tool.suffix())) {
                    matches.add(callback);
                }
            }
        }

        if (matches.isEmpty()) {
            throw new IllegalStateException("No MCP tool registered for suffix " + tool.suffix());
        }
        if (matches.size() > 1) {
            throw new IllegalStateException("Multiple MCP tools matched suffix " + tool.suffix());
        }
        return matches.getFirst();
    }
}
