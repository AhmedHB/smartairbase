package se.smartairbase.mcpclient.service.analysis;

import org.springframework.stereotype.Service;
import se.smartairbase.mcpclient.domain.AnalysisRole;

import java.util.List;

@Service
/**
 * Generates deterministic fallback narration from structured round facts.
 */
public class RuleBasedNarrationService {

    public AnalysisNarration narrate(AnalysisRole role, AnalysisRoundFacts facts) {
        return switch (role) {
            case PILOT -> {
                String landed = facts.landedAircraft().isEmpty()
                        ? "No aircraft returned this round"
                        : joinList(facts.landedAircraft()) + " touched down";
                String missions = facts.completedMissions() + " of " + facts.totalMissions() + " missions done";
                String holding = facts.holdingAircraft().isEmpty()
                        ? "None holding"
                        : joinList(facts.holdingAircraft()) + " still airborne";
                String destroyed = facts.destroyedThisRound().isEmpty()
                        ? "no losses"
                        : joinList(facts.destroyedThisRound()) + " lost this round";
                yield new AnalysisNarration("Rule-based",
                        landed + " \u2014 " + missions + ".",
                        holding + ", " + destroyed + ".");
            }
            case GROUND_CREW -> {
                String turnaround = facts.landedAircraft().isEmpty()
                        ? "No aircraft in for turnaround"
                        : joinList(facts.landedAircraft()) + " in for turnaround";
                String bases = "Resources updated at " + joinOrNone(facts.affectedBases());
                String holding = facts.holdingAircraft().isEmpty()
                        ? ""
                        : " " + joinList(facts.holdingAircraft()) + " still airborne.";
                yield new AnalysisNarration("Rule-based",
                        turnaround + ".",
                        bases + "." + holding);
            }
            case MAINTENANCE_TECHNICIANS -> {
                String repair = facts.underRepairAircraft().isEmpty()
                        ? "Nothing in the shop"
                        : joinList(facts.underRepairAircraft()) + " under repair";
                String waiting = facts.awaitingRepairAircraft().isEmpty()
                        ? "no backlog"
                        : joinList(facts.awaitingRepairAircraft()) + " waiting for a slot";
                String fullService = facts.fullServiceAircraft().isEmpty()
                        ? "No full-service requirements."
                        : joinList(facts.fullServiceAircraft()) + " flagged for full service.";
                yield new AnalysisNarration("Rule-based",
                        repair + ", " + waiting + ".",
                        fullService);
            }
            case COMMAND_OPERATIONS -> {
                String ready = facts.readyAircraftCount() == 1
                        ? "1 aircraft ready"
                        : facts.readyAircraftCount() + " aircraft ready";
                String losses = facts.destroyedAircraftCount() == 0
                        ? "no total losses"
                        : facts.destroyedAircraftCount() + " aircraft lost";
                String roundRef = facts.maxRounds() != null
                        ? "Round " + facts.round() + " of " + facts.maxRounds() + " concluded"
                        : "Round " + facts.round() + " concluded";
                String statusLine = "ACTIVE".equals(facts.gameStatus())
                        ? " \u2014 mission ongoing."
                        : " \u2014 " + facts.gameStatus().toLowerCase().replace("_", " ") + ".";
                yield new AnalysisNarration("Rule-based",
                        facts.completedMissions() + " of " + facts.totalMissions() + " missions complete \u2014 " + ready + ", " + losses + ".",
                        roundRef + statusLine);
            }
        };
    }

    private String joinOrNone(List<String> values) {
        return values.isEmpty() ? "none" : joinList(values);
    }

    private String joinList(List<String> values) {
        if (values.isEmpty()) return "none";
        if (values.size() == 1) return values.get(0);
        return String.join(", ", values.subList(0, values.size() - 1)) + " and " + values.get(values.size() - 1);
    }
}
