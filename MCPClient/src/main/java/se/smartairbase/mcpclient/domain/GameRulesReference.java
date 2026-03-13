package se.smartairbase.mcpclient.domain;

import java.util.List;

/**
 * Aggregates the published rules reference that the UI can render.
 */
public record GameRulesReference(
        String version,
        List<String> objectives,
        InitialSetupReference initialSetup,
        List<MissionReference> missions,
        List<BaseReference> bases,
        List<DiceOutcomeReference> diceOutcomes,
        ResourceRulesReference resourceRules,
        List<String> roundPhases
) {
}
