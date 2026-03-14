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
            case PILOT -> narratePilot(facts);
            case GROUND_CREW -> narrateGroundCrew(facts);
            case MAINTENANCE_TECHNICIANS -> narrateMaintenance(facts);
            case COMMAND_OPERATIONS -> narrateCommand(facts);
        };
    }

    private AnalysisNarration narratePilot(AnalysisRoundFacts facts) {
        String summary;
        if (facts.landedAircraft().isEmpty() && facts.holdingAircraft().isEmpty()) {
            summary = "Nobody touched down this round — everybody's still out there.";
        } else if (facts.landedAircraft().isEmpty()) {
            summary = join(facts.holdingAircraft()) + " couldn't land and went into holding.";
        } else {
            summary = join(facts.landedAircraft()) + " made it back this round.";
            if (!facts.holdingAircraft().isEmpty()) {
                summary += " " + join(facts.holdingAircraft()) + " is holding, waiting for a slot.";
            }
        }

        String details;
        if (!facts.destroyedThisRound().isEmpty()) {
            details = "We lost " + join(facts.destroyedThisRound()) + " this round. That one hurts.";
        } else {
            details = "No losses this round. " + facts.completedMissions() + " of " + facts.totalMissions() + " missions done, " + facts.missionsRemaining() + " to go.";
        }
        return new AnalysisNarration("Rule-based", summary, details);
    }

    private AnalysisNarration narrateGroundCrew(AnalysisRoundFacts facts) {
        String summary;
        boolean refueled = !facts.refueledAircraft().isEmpty();
        boolean rearmed = !facts.rearmedAircraft().isEmpty();
        if (!refueled && !rearmed) {
            summary = "Quiet round on the apron — no refuelling or rearming needed.";
        } else if (refueled && rearmed) {
            summary = "Refuelled " + join(facts.refueledAircraft()) + " and rearmed " + join(facts.rearmedAircraft()) + ".";
        } else if (refueled) {
            summary = "Topped off " + join(facts.refueledAircraft()) + " this round, no rearming needed.";
        } else {
            summary = "Rearmed " + join(facts.rearmedAircraft()) + " — no fuel runs this round.";
        }

        String details;
        if (facts.affectedBases().isEmpty()) {
            details = "No base resource changes this round.";
        } else {
            details = facts.affectedBases().size() == 1
                    ? facts.affectedBases().get(0) + " saw all the action this round."
                    : "Activity logged at " + join(facts.affectedBases()) + ".";
        }
        return new AnalysisNarration("Rule-based", summary, details);
    }

    private AnalysisNarration narrateMaintenance(AnalysisRoundFacts facts) {
        String summary;
        if (facts.underRepairAircraft().isEmpty() && facts.awaitingRepairAircraft().isEmpty()) {
            summary = "Nothing in the workshop right now — the fleet's holding up.";
        } else if (facts.underRepairAircraft().isEmpty()) {
            summary = join(facts.awaitingRepairAircraft()) + " is in the queue, waiting for a maintenance slot to open up.";
        } else if (facts.awaitingRepairAircraft().isEmpty()) {
            summary = join(facts.underRepairAircraft()) + " is on the bench getting worked on.";
        } else {
            summary = join(facts.underRepairAircraft()) + " is being worked on. " + join(facts.awaitingRepairAircraft()) + " is waiting for a slot.";
        }

        String details;
        if (!facts.fullServiceAircraft().isEmpty()) {
            details = join(facts.fullServiceAircraft()) + " needs a full service — that's going to take time.";
        } else {
            details = "No full-service cases this round.";
        }
        return new AnalysisNarration("Rule-based", summary, details);
    }

    private AnalysisNarration narrateCommand(AnalysisRoundFacts facts) {
        String progress = facts.completedMissions() + "/" + facts.totalMissions() + " missions complete";
        String summary;
        if (facts.missionsRemaining() == 0) {
            summary = progress + " — all objectives achieved.";
        } else {
            summary = progress + ", " + facts.missionsRemaining() + " remaining. "
                    + facts.readyAircraftCount() + " aircraft ready to fly.";
        }

        String details;
        if (!facts.destroyedThisRound().isEmpty()) {
            details = "We took losses this round: " + join(facts.destroyedThisRound()) + ". Total destroyed: " + facts.destroyedAircraftCount() + ".";
        } else if (facts.destroyedAircraftCount() > 0) {
            details = "No new losses this round. Running total of " + facts.destroyedAircraftCount() + " aircraft destroyed.";
        } else {
            details = "No losses on the board. Round " + facts.round() + " of " + facts.maxRounds() + " wrapped clean.";
        }
        return new AnalysisNarration("Rule-based", summary, details);
    }

    public AnalysisNarration narrateFinal(AnalysisRole role, AnalysisGameFacts facts) {
        boolean won = "WON".equals(facts.gameStatus());
        return switch (role) {
            case PILOT -> {
                String summary = won
                        ? "We got the job done — " + facts.completedMissions() + " missions over " + facts.totalRounds() + " rounds, and we brought everyone home we could."
                        : "We didn't make it. " + facts.completedMissions() + " of " + facts.totalMissions() + " missions completed before the campaign ended.";
                String details = facts.destroyedAircraftCount() == 0
                        ? "Not a single aircraft lost. The whole squadron made it through."
                        : facts.destroyedAircraftCount() + " aircraft lost, " + facts.survivingAircraftCount() + " still flying at the end.";
                yield new AnalysisNarration("Rule-based", summary, details);
            }
            case GROUND_CREW -> {
                String summary = won
                        ? "We kept them flying for " + facts.totalRounds() + " rounds straight. Every turnaround counted."
                        : "We worked every shift, but " + facts.totalRounds() + " rounds wasn't enough to close it out.";
                String details = facts.survivingAircraftCount() + " aircraft still operational at the end, " + facts.destroyedAircraftCount() + " lost beyond recovery.";
                yield new AnalysisNarration("Rule-based", summary, details);
            }
            case MAINTENANCE_TECHNICIANS -> {
                String summary = won
                        ? facts.destroyedAircraftCount() == 0
                            ? "Clean campaign — no losses, " + facts.totalRounds() + " rounds, and the workshop never got overwhelmed."
                            : facts.totalRounds() + " rounds of hard maintenance work. We kept " + facts.survivingAircraftCount() + " airframes flying."
                        : facts.destroyedAircraftCount() + " aircraft went beyond what we could fix. The campaign ended before we could recover.";
                String details = facts.survivingAircraftCount() + " aircraft remain airworthy. " + facts.destroyedAircraftCount() + " were lost beyond repair.";
                yield new AnalysisNarration("Rule-based", summary, details);
            }
            case COMMAND_OPERATIONS -> {
                String summary = won
                        ? "Campaign complete. " + facts.completedMissions() + "/" + facts.totalMissions() + " objectives achieved in " + facts.totalRounds() + " rounds."
                        : "Campaign ended short of the objective. " + facts.completedMissions() + " of " + facts.totalMissions() + " missions completed in " + facts.totalRounds() + " rounds.";
                String profile = facts.diceSelectionProfile() != null ? facts.diceSelectionProfile().replace("_", " ").toLowerCase() : "mixed";
                String details = facts.survivingAircraftCount() + " aircraft survived. Dice selection mode: " + profile + ".";
                yield new AnalysisNarration("Rule-based", summary, details);
            }
        };
    }

    private String join(List<String> values) {
        if (values.isEmpty()) return "none";
        if (values.size() == 1) return values.get(0);
        if (values.size() == 2) return values.get(0) + " and " + values.get(1);
        return String.join(", ", values.subList(0, values.size() - 1)) + ", and " + values.get(values.size() - 1);
    }
}
