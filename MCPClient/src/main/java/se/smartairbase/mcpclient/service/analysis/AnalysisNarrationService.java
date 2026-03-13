package se.smartairbase.mcpclient.service.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import se.smartairbase.mcpclient.domain.AnalysisRole;

@Service
/**
 * Selects between LLM-backed and rule-based narration generation.
 */
public class AnalysisNarrationService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisNarrationService.class);

    private final LlmNarrationService llmNarrationService;
    private final RuleBasedNarrationService ruleBasedNarrationService;

    public AnalysisNarrationService(LlmNarrationService llmNarrationService,
                                    RuleBasedNarrationService ruleBasedNarrationService) {
        this.llmNarrationService = llmNarrationService;
        this.ruleBasedNarrationService = ruleBasedNarrationService;
    }

    public AnalysisNarration narrate(AnalysisRole role, AnalysisRoundFacts facts) {
        // LLM output is optional. The client always has a deterministic fallback so
        // the feed can still be persisted even when narration generation fails.
        AnalysisNarration llmNarration = llmNarrationService.narrate(role, facts);
        if (llmNarration != null) {
            log.debug("Analysis narration for role {} in round {} generated via {}", role, facts.round(), llmNarration.source());
            return llmNarration;
        }
        AnalysisNarration fallback = ruleBasedNarrationService.narrate(role, facts);
        log.debug("Analysis narration for role {} in round {} fell back to {}", role, facts.round(), fallback.source());
        return fallback;
    }
}
