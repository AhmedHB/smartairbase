package se.smartairbase.mcpserver.service;

import org.springframework.stereotype.Service;
import se.smartairbase.mcpserver.domain.game.BaseState;
import se.smartairbase.mcpserver.domain.game.GameBase;
import se.smartairbase.mcpserver.mcp.dto.BaseStateDto;
import se.smartairbase.mcpserver.repository.BaseStateRepository;
import se.smartairbase.mcpserver.repository.GameBaseRepository;

@Service
/**
 * Read service for a single base and its current stocks/capacity usage.
 */
public class BaseService {

    private final GameBaseRepository gameBaseRepository;
    private final BaseStateRepository baseStateRepository;

    public BaseService(GameBaseRepository gameBaseRepository,
                       BaseStateRepository baseStateRepository) {
        this.gameBaseRepository = gameBaseRepository;
        this.baseStateRepository = baseStateRepository;
    }

    /**
     * Returns the current state of one base in a specific game.
     */
    public BaseStateDto getBaseState(Long gameId, String baseCode) {
        GameBase base = gameBaseRepository.findByGame_IdAndCode(gameId, baseCode)
                .orElseThrow(() -> new IllegalArgumentException("Base not found: " + baseCode));
        BaseState state = baseStateRepository.findByGameBase_Id(base.getId())
                .orElseThrow(() -> new IllegalArgumentException("Base state not found: " + baseCode));

        return new BaseStateDto(
                base.getCode(),
                base.getName(),
                base.getBaseType().getCode(),
                state.getFuelStock(),
                state.getWeaponsStock(),
                state.getSparePartsStock(),
                state.getOccupiedParkingSlots(),
                base.getParkingCapacity(),
                state.getOccupiedMaintSlots(),
                base.getMaintenanceCapacity()
        );
    }
}
