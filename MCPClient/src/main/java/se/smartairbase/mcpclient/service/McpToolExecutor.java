package se.smartairbase.mcpclient.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;
import se.smartairbase.mcpclient.domain.SmartAirBaseTool;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
/**
 * Thin execution layer for MCP tools discovered through Spring AI.
 *
 * <p>The client code addresses tools through {@link SmartAirBaseTool} rather than
 * raw callback names. This service resolves the matching callback, serializes the
 * request payload and parses the JSON response into a tree.</p>
 */
public class McpToolExecutor {
    private static final Pattern TEXT_CONTENT_MESSAGE = Pattern.compile("text=([^,\\]]+)");

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
            throw new IllegalArgumentException(extractErrorMessage(tool, exception), exception);
        }
    }

    private String extractErrorMessage(SmartAirBaseTool tool, Exception exception) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null
                    && !message.isBlank()
                    && !message.startsWith("Failed to execute MCP tool ")) {
                return normalizeErrorMessage(message);
            }
            current = current.getCause();
        }
        return "Failed to execute MCP tool " + tool.suffix();
    }

    private String normalizeErrorMessage(String message) {
        String normalized = message.trim();
        if (normalized.startsWith("Error calling tool:")) {
            Matcher matcher = TEXT_CONTENT_MESSAGE.matcher(normalized);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        return normalized;
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
