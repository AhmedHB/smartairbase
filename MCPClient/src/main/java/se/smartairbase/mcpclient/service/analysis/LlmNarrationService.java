package se.smartairbase.mcpclient.service.analysis;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.smartairbase.mcpclient.domain.AnalysisRole;

@Service
/**
 * Generates optional LLM-based round narration for one analysis role.
 */
public class LlmNarrationService {

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final RolePromptFactory rolePromptFactory;
    private final String mode;

    public LlmNarrationService(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
                               RolePromptFactory rolePromptFactory,
                               @Value("${smartairbase.analysis.narration-mode:hybrid}") String mode) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.rolePromptFactory = rolePromptFactory;
        this.mode = mode;
    }

    public AnalysisNarration narrate(AnalysisRole role, AnalysisRoundFacts facts) {
        if (!isEnabled()) {
            return null;
        }

        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            return null;
        }

        try {
            String content = builder.build()
                    .prompt()
                    .system(rolePromptFactory.systemPrompt(role))
                    .user(rolePromptFactory.userPrompt(role, facts))
                    .call()
                    .content();

            if (content == null || content.isBlank()) {
                return null;
            }
            return new AnalysisNarration("LLM", content.trim(), null);
        } catch (Exception ignored) {
            return null;
        }
    }

    public AnalysisNarration narrateFinal(AnalysisRole role, AnalysisGameFacts facts) {
        if (!isEnabled()) {
            return null;
        }
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            return null;
        }
        try {
            String content = builder.build()
                    .prompt()
                    .system(rolePromptFactory.finalSystemPrompt(role))
                    .user(rolePromptFactory.finalUserPrompt(role, facts))
                    .call()
                    .content();
            if (content == null || content.isBlank()) {
                return null;
            }
            return new AnalysisNarration("LLM", content.trim(), null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isEnabled() {
        return !"rule-based".equalsIgnoreCase(mode);
    }
}
