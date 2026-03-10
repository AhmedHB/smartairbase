package se.smartairbase.mcpserver.service;

import org.springframework.stereotype.Service;
import se.smartairbase.mcpserver.mcp.dto.ActionResultDto;

@Service
public class MissionAssignmentService {

    private final RoundService roundService;

    public MissionAssignmentService(RoundService roundService) {
        this.roundService = roundService;
    }

    public ActionResultDto assignMission(Long gameId, String aircraftCode, String missionCode) {
        return roundService.assignMission(gameId, aircraftCode, missionCode);
    }
}
