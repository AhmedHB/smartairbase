package se.smartairbase.mcpclient.service.analysis;

import org.springframework.stereotype.Service;
import se.smartairbase.mcpclient.domain.AnalysisRole;

@Service
/**
 * Generates deterministic fallback narration from structured round facts.
 */
public class RuleBasedNarrationService {

    public AnalysisNarration narrate(AnalysisRole role, AnalysisRoundFacts facts) {
        return switch (role) {
            case PILOT -> new AnalysisNarration(
                    "Rule-based",
                    facts.completedMissions() + "/" + facts.totalMissions() + " missions complete. Returned aircraft: " + joinOrNone(facts.landedAircraft()) + ".",
                    "Holding: " + joinOrNone(facts.holdingAircraft()) + ". Destroyed this round: " + joinOrNone(facts.destroyedThisRound()) + "."
            );
            case GROUND_CREW -> new AnalysisNarration(
                    "Rule-based",
                    "Refuel observed on " + joinOrNone(facts.refueledAircraft()) + ". Rearm observed on " + joinOrNone(facts.rearmedAircraft()) + ".",
                    "Base resource changes detected at " + joinOrNone(facts.affectedBases()) + "."
            );
            case MAINTENANCE_TECHNICIANS -> new AnalysisNarration(
                    "Rule-based",
                    "Maintenance pressure aircraft: " + joinOrNone(facts.maintenanceAircraft()) + ".",
                    "Aircraft requiring full service: " + joinOrNone(facts.fullServiceAircraft()) + "."
            );
            case COMMAND_OPERATIONS -> new AnalysisNarration(
                    "Rule-based",
                    facts.completedMissions() + "/" + facts.totalMissions() + " missions complete. " + facts.readyAircraftCount() + " aircraft ready. " + facts.destroyedAircraftCount() + " destroyed.",
                    "Round " + facts.round() + " ended in " + phaseText(facts.phase()) + " with game status " + facts.gameStatus() + "."
            );
        };
    }

    private String joinOrNone(java.util.List<String> values) {
        return values.isEmpty() ? "none" : String.join(", ", values);
    }

    private String phaseText(String phase) {
        return phase != null ? phase : "ROUND_COMPLETE";
    }
}
