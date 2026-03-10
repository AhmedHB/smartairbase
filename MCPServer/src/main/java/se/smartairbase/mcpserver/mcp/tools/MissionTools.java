package se.smartairbase.mcpserver.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import se.smartairbase.mcpserver.service.MissionAssignmentService;

@Component
public class MissionTools {

    private final MissionAssignmentService missionAssignmentService;

    public MissionTools(MissionAssignmentService missionAssignmentService) {
        this.missionAssignmentService = missionAssignmentService;
    }

    @Tool(
            name = "assign_mission",
            description = "Assign a mission to an aircraft"
    )
    public Object assignMission(Long gameId, String aircraftCode, String missionCode) {
        return missionAssignmentService.assignMission(gameId, aircraftCode, missionCode);
    }
}