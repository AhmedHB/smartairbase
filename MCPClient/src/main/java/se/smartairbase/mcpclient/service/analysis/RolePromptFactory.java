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
                    Write one short, in-character remark about what you just experienced this round.
                    You speak from the cockpit's perspective: sorties flown, how the aircraft felt, landing or holding situation, aircrew welfare.
                    You are not a manager — you don't track mission counts or base inventory. You know only what you flew and saw.
                    Match your tone to the situation: tight and terse when things go badly, dry and matter-of-fact otherwise.
                    Do not invent facts not present in the supplied round facts.
                    1-2 sentences maximum. Plain text only.
                    """;
            case GROUND_CREW -> """
                    You are Sara Lind, Ground Crew Chief in SmartAirBase.
                    Write one short, in-character remark about what your crew just dealt with this round.
                    Your world is the flight line: refuelling, rearming, towing aircraft, managing parking pressure at the bases.
                    You don't track mission objectives or command decisions — you track how hard your people are working and whether you kept up with demand.
                    Sound hands-on and direct. Use informal crew language, not military formality.
                    Do not invent facts not present in the supplied round facts.
                    1-2 sentences maximum. Plain text only.
                    """;
            case MAINTENANCE_TECHNICIANS -> """
                    You are Johan Berg, Lead Maintenance Technician in SmartAirBase.
                    Write one short, in-character remark about what landed on your workshop this round.
                    Your focus is aircraft damage, repair schedules, full-service workload, and whether spare parts are holding up.
                    You are not concerned with missions or base fuel — you are concerned with metal, hours, and parts availability.
                    Sound precise and a little dry. You deal in facts and timelines, not feelings.
                    Do not invent facts not present in the supplied round facts.
                    1-2 sentences maximum. Plain text only.
                    """;
            case COMMAND_OPERATIONS -> """
                    You are Colonel Anna Sjöberg, Command / Operations in SmartAirBase.
                    Write one short, in-character operational assessment of where things stand after this round.
                    You track the full picture: mission completion rate, how many aircraft are ready, tempo, and whether risk is increasing or decreasing.
                    You are decisive and economical with words. You don't explain — you assess and sometimes direct.
                    Raise your urgency when aircraft are being lost or missions are stalling. Be controlled but not cheerful when things are going well.
                    Do not invent facts not present in the supplied round facts.
                    1-2 sentences maximum. Plain text only.
                    """;
        };
    }

    public String userPrompt(AnalysisRole role, AnalysisRoundFacts facts) {
        return switch (role) {
            case PILOT -> "Round " + facts.round() + " facts for Captain Erik Holm:\n"
                    + "gameStatus=" + facts.gameStatus() + "\n"
                    + "missionsRemaining=" + facts.missionsRemaining() + "\n"
                    + "landedAircraft=" + facts.landedAircraft() + "\n"
                    + "holdingAircraft=" + facts.holdingAircraft() + "\n"
                    + "destroyedThisRound=" + facts.destroyedThisRound() + "\n"
                    + "readyAircraftCount=" + facts.readyAircraftCount() + "\n"
                    + "destroyedAircraftCount=" + facts.destroyedAircraftCount() + "\n"
                    + "\nReturn plain text only.";
            case GROUND_CREW -> "Round " + facts.round() + " facts for Sara Lind:\n"
                    + "gameStatus=" + facts.gameStatus() + "\n"
                    + "landedThisRound=" + facts.landedAircraft() + "\n"
                    + "holdingAircraft=" + facts.holdingAircraft() + "\n"
                    + "affectedBases=" + facts.affectedBases() + "\n"
                    + "keyBases=" + facts.keyBases() + "\n"
                    + "\nReturn plain text only.";
            case MAINTENANCE_TECHNICIANS -> "Round " + facts.round() + " facts for Johan Berg:\n"
                    + "gameStatus=" + facts.gameStatus() + "\n"
                    + "totalAircraftCount=" + facts.totalAircraftCount() + "\n"
                    + "underRepairAircraft=" + facts.underRepairAircraft() + "\n"
                    + "awaitingRepairAircraft=" + facts.awaitingRepairAircraft() + "\n"
                    + "fullServiceAircraft=" + facts.fullServiceAircraft() + "\n"
                    + "destroyedThisRound=" + facts.destroyedThisRound() + "\n"
                    + "destroyedAircraftCount=" + facts.destroyedAircraftCount() + "\n"
                    + "\nReturn plain text only.";
            case COMMAND_OPERATIONS -> "Round " + facts.round()
                    + (facts.maxRounds() != null ? "/" + facts.maxRounds() : "") + " facts for Colonel Anna Sjöberg:\n"
                    + "scenario=" + facts.scenarioName() + "\n"
                    + "gameStatus=" + facts.gameStatus() + "\n"
                    + "completedMissions=" + facts.completedMissions() + "/" + facts.totalMissions() + "\n"
                    + "missionsRemaining=" + facts.missionsRemaining() + "\n"
                    + "readyAircraftCount=" + facts.readyAircraftCount() + "\n"
                    + "totalAircraftCount=" + facts.totalAircraftCount() + "\n"
                    + "destroyedAircraftCount=" + facts.destroyedAircraftCount() + "\n"
                    + "destroyedThisRound=" + facts.destroyedThisRound() + "\n"
                    + "holdingAircraft=" + facts.holdingAircraft() + "\n"
                    + "underRepairAircraft=" + facts.underRepairAircraft() + "\n"
                    + "awaitingRepairAircraft=" + facts.awaitingRepairAircraft() + "\n"
                    + "\nReturn plain text only.";
        };
    }
}
