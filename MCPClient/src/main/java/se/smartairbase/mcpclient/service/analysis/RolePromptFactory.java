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
                    You are the Pilot voice in SmartAirBase.
                    Write a short improvised action comment about what the pilot side just experienced.
                    Focus on sorties, aircraft condition, landing outcome, and immediate aircrew situation.
                    Do not invent facts that are not present in the supplied round facts.
                    Sound like a human operational voice, not a system report.
                    Limit yourself to 1-2 short sentences.
                    """;
            case GROUND_CREW -> """
                    You are the Ground Crew voice in SmartAirBase.
                    Write a short improvised action comment about what the ground crew just handled.
                    Focus on refuel, rearm, turnaround work, and base pressure.
                    Do not invent facts that are not present in the supplied round facts.
                    Sound practical, hands-on, and human.
                    Limit yourself to 1-2 short sentences.
                    """;
            case MAINTENANCE_TECHNICIANS -> """
                    You are the Maintenance Technicians voice in SmartAirBase.
                    Write a short improvised action comment about what the technicians are dealing with right now.
                    Focus on repairs, full service, spare parts pressure, and workshop load.
                    Do not invent facts that are not present in the supplied round facts.
                    Sound technical, direct, and human.
                    Limit yourself to 1-2 short sentences.
                    """;
            case COMMAND_OPERATIONS -> """
                    You are the Command / Operations voice in SmartAirBase.
                    Write a short improvised command comment about the current operational picture.
                    Focus on mission progress, readiness, tempo, and current risk.
                    Do not invent facts that are not present in the supplied round facts.
                    Sound like a real operations lead, concise but not robotic.
                    Limit yourself to 1-2 short sentences.
                    """;
        };
    }

    public String userPrompt(AnalysisRole role, AnalysisRoundFacts facts) {
        return "Role: " + role.displayName() + "\n"
                + "Round facts:\n"
                + "round=" + facts.round() + "\n"
                + "phase=" + (facts.phase() != null ? facts.phase() : "ROUND_COMPLETE") + "\n"
                + "gameStatus=" + facts.gameStatus() + "\n"
                + "completedMissions=" + facts.completedMissions() + "/" + facts.totalMissions() + "\n"
                + "readyAircraftCount=" + facts.readyAircraftCount() + "\n"
                + "destroyedAircraftCount=" + facts.destroyedAircraftCount() + "\n"
                + "landedAircraft=" + facts.landedAircraft() + "\n"
                + "holdingAircraft=" + facts.holdingAircraft() + "\n"
                + "destroyedThisRound=" + facts.destroyedThisRound() + "\n"
                + "maintenanceAircraft=" + facts.maintenanceAircraft() + "\n"
                + "fullServiceAircraft=" + facts.fullServiceAircraft() + "\n"
                + "refueledAircraft=" + facts.refueledAircraft() + "\n"
                + "rearmedAircraft=" + facts.rearmedAircraft() + "\n"
                + "affectedBases=" + facts.affectedBases() + "\n"
                + "keyAircraft=" + facts.keyAircraft() + "\n"
                + "keyBases=" + facts.keyBases() + "\n"
                + "\nReturn plain text only.";
    }
}
