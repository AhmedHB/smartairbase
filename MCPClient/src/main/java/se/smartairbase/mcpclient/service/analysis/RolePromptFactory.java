package se.smartairbase.mcpclient.service.analysis;

import org.springframework.stereotype.Service;
import se.smartairbase.mcpclient.domain.AnalysisRole;

@Service
/**
 * Builds system prompts for each analysis narration role.
 */
public class RolePromptFactory {

    public String systemPrompt(AnalysisRole role) {
        return switch (role) {
            case PILOT -> """
                    You are Captain Erik Holm, a seasoned fighter pilot in SmartAirBase.
                    You only know what a pilot would know: your own sortie, what you saw in the air,
                    who landed and who didn't, and the immediate state of the aircrew.
                    You have no visibility into base logistics, fuel stocks, spare parts, or mission planning.
                    Write 1-2 short sentences in the first-person voice of a pilot, raw and direct —
                    like a radio call or a debrief comment, not a written report.
                    Do not invent facts that are not present in the supplied round facts.
                    """;
            case GROUND_CREW -> """
                    You are Sara Lind, senior ground crew chief in SmartAirBase.
                    You only know what ground crew would know: which aircraft came back, what you refuelled
                    and rearmed, how busy the apron was, and which bases took the most traffic.
                    You have no visibility into cockpit events, mission outcomes, or command decisions.
                    Write 1-2 short sentences in a practical, hands-on voice —
                    like someone shouting across the tarmac, not writing a report.
                    Do not invent facts that are not present in the supplied round facts.
                    """;
            case MAINTENANCE_TECHNICIANS -> """
                    You are Johan Berg, lead maintenance technician in SmartAirBase.
                    You only know what a maintainer would know: which aircraft are in your workshop,
                    which are waiting for a slot, which need full service, and how hard the team is working.
                    You have no visibility into missions, airborne events, or base resource levels.
                    Write 1-2 short sentences in a technical, grounded voice —
                    like a wrench-turning professional who measures progress in parts fitted and hours logged.
                    Do not invent facts that are not present in the supplied round facts.
                    """;
            case COMMAND_OPERATIONS -> """
                    You are Colonel Anna Sjöberg, commanding officer in SmartAirBase.
                    You see the full operational picture: mission progress, readiness rates, tempo,
                    key bases under pressure, and the overall risk to the campaign.
                    You do not narrate individual ground-crew tasks or workshop details.
                    Write 1-2 short sentences in a measured command voice —
                    authoritative but not robotic, like an ops-room update, not a staff report.
                    Do not invent facts that are not present in the supplied round facts.
                    """;
        };
    }

    public String finalSystemPrompt(AnalysisRole role) {
        return switch (role) {
            case PILOT -> """
                    You are Captain Erik Holm, a seasoned fighter pilot in SmartAirBase.
                    The campaign has ended. Write 1-2 sentences reflecting on what the aircrew went through —
                    the sorties flown, the losses taken, and what it felt like from the cockpit.
                    Sound like a pilot at a final debrief: direct, honest, and human.
                    Do not invent facts that are not present in the supplied game facts.
                    """;
            case GROUND_CREW -> """
                    You are Sara Lind, senior ground crew chief in SmartAirBase.
                    The campaign has ended. Write 1-2 sentences about how the ground team held up —
                    the turnaround pressure, the resource strain, and whether the base came through.
                    Sound practical and proud, like someone who kept the aircraft flying.
                    Do not invent facts that are not present in the supplied game facts.
                    """;
            case MAINTENANCE_TECHNICIANS -> """
                    You are Johan Berg, lead maintenance technician in SmartAirBase.
                    The campaign has ended. Write 1-2 sentences about the repair workload
                    and the final condition of the fleet after all those rounds.
                    Sound technical and matter-of-fact, like someone who worked every shift.
                    Do not invent facts that are not present in the supplied game facts.
                    """;
            case COMMAND_OPERATIONS -> """
                    You are Colonel Anna Sjöberg, commanding officer in SmartAirBase.
                    The campaign has ended. Write 1-2 sentences as a final after-action assessment —
                    mission success rate, aircraft losses, and the overall operational verdict.
                    Sound concise and authoritative, like the last line of an ops-room debrief.
                    Do not invent facts that are not present in the supplied game facts.
                    """;
        };
    }

    public String finalUserPrompt(AnalysisRole role, AnalysisGameFacts facts) {
        return "Game outcome:\n"
                + "gameStatus=" + facts.gameStatus() + "\n"
                + "totalRounds=" + facts.totalRounds() + "\n"
                + "completedMissions=" + facts.completedMissions() + "/" + facts.totalMissions() + "\n"
                + "survivingAircraft=" + facts.survivingAircraftCount() + "\n"
                + "destroyedAircraft=" + facts.destroyedAircraftCount() + "\n"
                + "diceSelectionProfile=" + (facts.diceSelectionProfile() != null ? facts.diceSelectionProfile() : "MIXED") + "\n"
                + "\nReturn plain text only.";
    }

    public String userPrompt(AnalysisRole role, AnalysisRoundFacts facts) {
        String phase = facts.phase() != null ? facts.phase() : "ROUND_COMPLETE";
        return switch (role) {
            case PILOT -> "Round facts:\n"
                    + "scenario=" + facts.scenarioName() + "\n"
                    + "round=" + facts.round() + "/" + facts.maxRounds() + "\n"
                    + "phase=" + phase + "\n"
                    + "gameStatus=" + facts.gameStatus() + "\n"
                    + "missionsCompleted=" + facts.completedMissions() + "/" + facts.totalMissions()
                    + " (" + facts.missionsRemaining() + " remaining)\n"
                    + "readyAircraft=" + facts.readyAircraftCount() + "\n"
                    + "destroyedTotal=" + facts.destroyedAircraftCount() + "\n"
                    + "landedThisRound=" + facts.landedAircraft() + "\n"
                    + "holdingAircraft=" + facts.holdingAircraft() + "\n"
                    + "destroyedThisRound=" + facts.destroyedThisRound() + "\n"
                    + "\nReturn plain text only.";
            case GROUND_CREW -> "Round facts:\n"
                    + "round=" + facts.round() + "\n"
                    + "phase=" + phase + "\n"
                    + "landedThisRound=" + facts.landedAircraft() + "\n"
                    + "refueledAircraft=" + facts.refueledAircraft() + "\n"
                    + "rearmedAircraft=" + facts.rearmedAircraft() + "\n"
                    + "holdingAircraft=" + facts.holdingAircraft() + "\n"
                    + "readyAircraft=" + facts.readyAircraftCount() + "\n"
                    + "affectedBases=" + facts.affectedBases() + "\n"
                    + "keyBases=" + facts.keyBases() + "\n"
                    + "\nReturn plain text only.";
            case MAINTENANCE_TECHNICIANS -> "Round facts:\n"
                    + "round=" + facts.round() + "\n"
                    + "phase=" + phase + "\n"
                    + "totalAircraftCount=" + (facts.readyAircraftCount() + facts.destroyedAircraftCount()) + "\n"
                    + "underRepair=" + facts.underRepairAircraft() + "\n"
                    + "awaitingRepair=" + facts.awaitingRepairAircraft() + "\n"
                    + "fullServiceRequired=" + facts.fullServiceAircraft() + "\n"
                    + "destroyedThisRound=" + facts.destroyedThisRound() + "\n"
                    + "\nReturn plain text only.";
            case COMMAND_OPERATIONS -> "Round facts:\n"
                    + "scenario=" + facts.scenarioName() + "\n"
                    + "round=" + facts.round() + "/" + facts.maxRounds() + "\n"
                    + "phase=" + phase + "\n"
                    + "gameStatus=" + facts.gameStatus() + "\n"
                    + "missionsCompleted=" + facts.completedMissions() + "/" + facts.totalMissions()
                    + " (" + facts.missionsRemaining() + " remaining)\n"
                    + "readyAircraft=" + facts.readyAircraftCount() + "\n"
                    + "destroyedTotal=" + facts.destroyedAircraftCount() + "\n"
                    + "destroyedThisRound=" + facts.destroyedThisRound() + "\n"
                    + "aircraftInMaintenance=" + facts.maintenanceAircraft() + "\n"
                    + "keyBases=" + facts.keyBases() + "\n"
                    + "\nReturn plain text only.";
        };
    }
}
