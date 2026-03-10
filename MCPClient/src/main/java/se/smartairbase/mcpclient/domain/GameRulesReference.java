package se.smartairbase.mcpclient.domain;

import java.util.List;

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
