package se.smartairbase.mcpserver.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.smartairbase.mcpserver.domain.rule.RepairRule;
import se.smartairbase.mcpserver.domain.rule.Scenario;
import se.smartairbase.mcpserver.domain.rule.ScenarioAircraft;
import se.smartairbase.mcpserver.domain.rule.ScenarioBase;
import se.smartairbase.mcpserver.domain.rule.ScenarioMission;
import se.smartairbase.mcpserver.domain.rule.ScenarioSupplyRule;
import se.smartairbase.mcpserver.domain.rule.enums.ScenarioSourceType;
import se.smartairbase.mcpserver.mcp.dto.GameSummaryDto;
import se.smartairbase.mcpserver.mcp.dto.ActionResultDto;
import se.smartairbase.mcpserver.mcp.dto.ScenarioAircraftDto;
import se.smartairbase.mcpserver.mcp.dto.ScenarioBaseDto;
import se.smartairbase.mcpserver.mcp.dto.ScenarioDefinitionDto;
import se.smartairbase.mcpserver.mcp.dto.ScenarioDiceRuleDto;
import se.smartairbase.mcpserver.mcp.dto.ScenarioMissionDto;
import se.smartairbase.mcpserver.mcp.dto.ScenarioSummaryDto;
import se.smartairbase.mcpserver.mcp.dto.ScenarioSupplyRuleDto;
import se.smartairbase.mcpserver.mcp.dto.UpdateScenarioRequestDto;
import se.smartairbase.mcpserver.repository.RepairRuleRepository;
import se.smartairbase.mcpserver.repository.ScenarioAircraftRepository;
import se.smartairbase.mcpserver.repository.ScenarioBaseRepository;
import se.smartairbase.mcpserver.repository.ScenarioMissionRepository;
import se.smartairbase.mcpserver.repository.ScenarioRepository;
import se.smartairbase.mcpserver.repository.ScenarioSupplyRuleRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class ScenarioService {

    private static final String SCENARIO_NAME_PATTERN = "^[A-Z0-9_]+$";

    private final ScenarioRepository scenarioRepository;
    private final ScenarioBaseRepository scenarioBaseRepository;
    private final ScenarioAircraftRepository scenarioAircraftRepository;
    private final ScenarioMissionRepository scenarioMissionRepository;
    private final ScenarioSupplyRuleRepository scenarioSupplyRuleRepository;
    private final RepairRuleRepository repairRuleRepository;
    private final GameService gameService;

    public ScenarioService(ScenarioRepository scenarioRepository,
                           ScenarioBaseRepository scenarioBaseRepository,
                           ScenarioAircraftRepository scenarioAircraftRepository,
                           ScenarioMissionRepository scenarioMissionRepository,
                           ScenarioSupplyRuleRepository scenarioSupplyRuleRepository,
                           RepairRuleRepository repairRuleRepository,
                           GameService gameService) {
        this.scenarioRepository = scenarioRepository;
        this.scenarioBaseRepository = scenarioBaseRepository;
        this.scenarioAircraftRepository = scenarioAircraftRepository;
        this.scenarioMissionRepository = scenarioMissionRepository;
        this.scenarioSupplyRuleRepository = scenarioSupplyRuleRepository;
        this.repairRuleRepository = repairRuleRepository;
        this.gameService = gameService;
    }

    @Transactional(readOnly = true)
    public List<ScenarioSummaryDto> listScenarios() {
        return scenarioRepository.findAll().stream()
                .sorted(Comparator.comparing((Scenario scenario) -> scenario.getSourceType().name())
                        .thenComparing(Scenario::getName)
                        .thenComparing(Scenario::getVersion))
                .map(this::toSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScenarioDefinitionDto getScenario(Long scenarioId) {
        return toDefinitionDto(loadScenario(scenarioId));
    }

    @Transactional
    public ScenarioDefinitionDto duplicateScenario(Long scenarioId, String requestedName) {
        Scenario source = loadScenario(scenarioId);
        String duplicateName = normalizeDuplicateName(source, requestedName);
        if (scenarioRepository.existsByNameAndVersion(duplicateName, source.getVersion())) {
            throw new IllegalArgumentException("Scenario already exists: " + duplicateName + " " + source.getVersion());
        }

        LocalDateTime now = LocalDateTime.now();
        Scenario duplicate = scenarioRepository.save(new Scenario(
                duplicateName,
                source.getVersion(),
                source.getDescription(),
                ScenarioSourceType.USER,
                true,
                true,
                false,
                now,
                now
        ));

        List<ScenarioBase> sourceBases = scenarioBaseRepository.findByScenario_Id(source.getId());
        for (ScenarioBase base : sourceBases) {
            ScenarioBase duplicateBase = scenarioBaseRepository.save(new ScenarioBase(
                    duplicate,
                    base.getCode(),
                    base.getName(),
                    base.getBaseType(),
                    base.getParkingCapacity(),
                    base.getMaintenanceCapacity(),
                    base.getFuelStart(),
                    base.getFuelMax(),
                    base.getWeaponsStart(),
                    base.getWeaponsMax(),
                    base.getSparePartsStart(),
                    base.getSparePartsMax()
            ));
            for (ScenarioSupplyRule supplyRule : scenarioSupplyRuleRepository.findByScenarioBase_Id(base.getId())) {
                scenarioSupplyRuleRepository.save(new ScenarioSupplyRule(
                        duplicateBase,
                        supplyRule.getResource(),
                        supplyRule.getFrequencyRounds(),
                        supplyRule.getDeliveryAmount()
                ));
            }
        }

        for (ScenarioAircraft aircraft : scenarioAircraftRepository.findByScenario_Id(source.getId())) {
            scenarioAircraftRepository.save(new ScenarioAircraft(
                    duplicate,
                    aircraft.getCode(),
                    aircraft.getAircraftType(),
                    aircraft.getStartBaseCode(),
                    aircraft.getFuelStart(),
                    aircraft.getWeaponsStart(),
                    aircraft.getFlightHoursStart()
            ));
        }

        for (ScenarioMission mission : scenarioMissionRepository.findByScenario_IdOrderBySortOrder(source.getId())) {
            scenarioMissionRepository.save(new ScenarioMission(
                    duplicate,
                    mission.getCode(),
                    mission.getMissionType(),
                    mission.getSortOrder(),
                    mission.getDefaultCount(),
                    mission.getFuelCost(),
                    mission.getWeaponCost(),
                    mission.getFlightTimeCost()
            ));
        }

        return toDefinitionDto(duplicate);
    }

    @Transactional
    public ScenarioDefinitionDto updateScenario(Long scenarioId, UpdateScenarioRequestDto request) {
        Scenario scenario = loadScenario(scenarioId);
        if (!scenario.isEditable() || scenario.getSourceType() != ScenarioSourceType.USER) {
            throw new IllegalArgumentException("Scenario cannot be edited: " + scenario.getName());
        }
        scenario.updateDescription(request.description(), LocalDateTime.now());

        Map<String, ScenarioBase> basesByCode = new HashMap<>();
        for (ScenarioBase base : scenarioBaseRepository.findByScenario_Id(scenarioId)) {
            basesByCode.put(base.getCode(), base);
        }
        request.bases().forEach(baseUpdate -> {
            ScenarioBase base = basesByCode.get(baseUpdate.code());
            if (base == null) {
                throw new IllegalArgumentException("Unknown base in scenario update: " + baseUpdate.code());
            }
            int maintenanceCapacity = sanitizeBaseMaintenanceCapacity(baseUpdate.code(), baseUpdate.baseTypeCode(), baseUpdate.maintenanceCapacity());
            base.updateCapacity(
                    sanitizeNonNegative(baseUpdate.parkingCapacity(), "parkingCapacity"),
                    maintenanceCapacity
            );
        });

        Map<String, ScenarioAircraft> aircraftByCode = new HashMap<>();
        for (ScenarioAircraft aircraft : scenarioAircraftRepository.findByScenario_Id(scenarioId)) {
            aircraftByCode.put(aircraft.getCode(), aircraft);
        }
        request.aircraft().forEach(aircraftUpdate -> {
            ScenarioAircraft aircraft = aircraftByCode.get(aircraftUpdate.code());
            if (aircraft == null) {
                throw new IllegalArgumentException("Unknown aircraft in scenario update: " + aircraftUpdate.code());
            }
            if (aircraftUpdate.startBaseCode() != null && !basesByCode.containsKey(aircraftUpdate.startBaseCode())) {
                throw new IllegalArgumentException("Unknown start base for aircraft " + aircraftUpdate.code() + ": " + aircraftUpdate.startBaseCode());
            }
            aircraft.updateSetup(
                    aircraftUpdate.startBaseCode(),
                    sanitizeNonNegative(aircraftUpdate.fuelStart(), "fuelStart"),
                    sanitizeNonNegative(aircraftUpdate.weaponsStart(), "weaponsStart"),
                    sanitizeNonNegative(aircraftUpdate.flightHoursStart(), "flightHoursStart")
            );
        });

        Map<String, ScenarioMission> missionsByCode = new HashMap<>();
        for (ScenarioMission mission : scenarioMissionRepository.findByScenario_IdOrderBySortOrder(scenarioId)) {
            missionsByCode.put(mission.getCode(), mission);
        }
        request.missions().forEach(missionUpdate -> {
            ScenarioMission mission = missionsByCode.get(missionUpdate.code());
            if (mission == null) {
                throw new IllegalArgumentException("Unknown mission in scenario update: " + missionUpdate.code());
            }
            mission.updateSetup(
                    sanitizeNonNegative(missionUpdate.fuelCost(), "fuelCost"),
                    sanitizeNonNegative(missionUpdate.weaponCost(), "weaponCost"),
                    sanitizeNonNegative(missionUpdate.flightTimeCost(), "flightTimeCost")
            );
        });

        return toDefinitionDto(scenario);
    }

    @Transactional
    public GameSummaryDto createGameFromScenario(Long scenarioId, String gameName) {
        Scenario scenario = loadScenario(scenarioId);
        return gameService.createGameFromScenario(scenario.getName(), scenario.getVersion(), gameName);
    }

    @Transactional
    public ActionResultDto deleteScenario(Long scenarioId) {
        Scenario scenario = loadScenario(scenarioId);
        if (!scenario.isDeletable() || scenario.getSourceType() != ScenarioSourceType.USER) {
            throw new IllegalArgumentException("Scenario cannot be deleted: " + scenario.getName());
        }
        List<ScenarioBase> bases = scenarioBaseRepository.findByScenario_Id(scenarioId);
        for (ScenarioBase base : bases) {
            scenarioSupplyRuleRepository.deleteAll(scenarioSupplyRuleRepository.findByScenarioBase_Id(base.getId()));
        }
        scenarioAircraftRepository.deleteAll(scenarioAircraftRepository.findByScenario_Id(scenarioId));
        scenarioMissionRepository.deleteAll(scenarioMissionRepository.findByScenario_IdOrderBySortOrder(scenarioId));
        scenarioBaseRepository.deleteAll(bases);
        scenarioRepository.delete(scenario);
        return new ActionResultDto(true, "Scenario deleted");
    }

    private Scenario loadScenario(Long scenarioId) {
        return scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("Scenario not found: " + scenarioId));
    }

    private ScenarioSummaryDto toSummaryDto(Scenario scenario) {
        return new ScenarioSummaryDto(
                scenario.getId(),
                scenario.getName(),
                scenario.getVersion(),
                scenario.getSourceType().name(),
                scenario.isEditable(),
                scenario.isDeletable(),
                scenario.isPublished()
        );
    }

    private ScenarioDefinitionDto toDefinitionDto(Scenario scenario) {
        List<ScenarioBase> bases = scenarioBaseRepository.findByScenario_Id(scenario.getId()).stream()
                .sorted(Comparator.comparing(ScenarioBase::getCode))
                .toList();
        List<ScenarioAircraft> aircraft = scenarioAircraftRepository.findByScenario_Id(scenario.getId()).stream()
                .sorted(Comparator.comparing(ScenarioAircraft::getCode))
                .toList();
        List<ScenarioMission> missions = scenarioMissionRepository.findByScenario_IdOrderBySortOrder(scenario.getId());
        List<RepairRule> diceRules = repairRuleRepository.findAll().stream()
                .sorted(Comparator.comparing(RepairRule::getDiceValue))
                .toList();

        return new ScenarioDefinitionDto(
                scenario.getId(),
                scenario.getName(),
                scenario.getVersion(),
                scenario.getDescription(),
                scenario.getSourceType().name(),
                scenario.isEditable(),
                scenario.isDeletable(),
                scenario.isPublished(),
                bases.stream().map(this::toBaseDto).toList(),
                aircraft.stream().map(aircraftEntry -> new ScenarioAircraftDto(
                        aircraftEntry.getCode(),
                        aircraftEntry.getAircraftType().getCode(),
                        aircraftEntry.getStartBaseCode(),
                        aircraftEntry.getFuelStart(),
                        aircraftEntry.getWeaponsStart(),
                        aircraftEntry.getFlightHoursStart()
                )).toList(),
                missions.stream().map(mission -> new ScenarioMissionDto(
                        mission.getCode(),
                        mission.getMissionType().getCode(),
                        mission.getMissionType().getName(),
                        mission.getSortOrder(),
                        mission.getDefaultCount(),
                        mission.getFuelCost(),
                        mission.getWeaponCost(),
                        mission.getFlightTimeCost()
                )).toList(),
                diceRules.stream().map(rule -> new ScenarioDiceRuleDto(
                        rule.getDiceValue(),
                        rule.getDamage().name(),
                        rule.getSparePartsCost(),
                        rule.getRepairRounds(),
                        rule.isRequiresFullService()
                )).toList()
        );
    }

    private ScenarioBaseDto toBaseDto(ScenarioBase base) {
        List<ScenarioSupplyRuleDto> supplyRules = scenarioSupplyRuleRepository.findByScenarioBase_Id(base.getId()).stream()
                .sorted(Comparator.comparing(rule -> rule.getResource().name()))
                .map(rule -> new ScenarioSupplyRuleDto(rule.getResource().name(), rule.getFrequencyRounds(), rule.getDeliveryAmount()))
                .toList();
        return new ScenarioBaseDto(
                base.getCode(),
                base.getName(),
                base.getBaseType().getCode(),
                base.getParkingCapacity(),
                base.getMaintenanceCapacity(),
                base.getFuelStart(),
                base.getFuelMax(),
                base.getWeaponsStart(),
                base.getWeaponsMax(),
                base.getSparePartsStart(),
                base.getSparePartsMax(),
                supplyRules
        );
    }

    private String normalizeDuplicateName(Scenario source, String requestedName) {
        if (requestedName != null && !requestedName.isBlank()) {
            String normalized = requestedName.trim().toUpperCase();
            validateScenarioName(normalized);
            return normalized;
        }
        String defaultName = source.getName().toUpperCase() + "_COPY";
        validateScenarioName(defaultName);
        return defaultName;
    }

    private int sanitizeNonNegative(Integer value, String fieldName) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(fieldName + " must be zero or higher");
        }
        return value;
    }

    private int sanitizeBaseMaintenanceCapacity(String baseCode, String baseTypeCode, Integer value) {
        int maintenanceCapacity = sanitizeNonNegative(value, "maintenanceCapacity");
        String normalizedBaseCode = baseCode == null ? "" : baseCode.trim().toUpperCase();
        String normalizedBaseTypeCode = baseTypeCode == null ? "" : baseTypeCode.trim().toUpperCase();
        if ("C".equals(normalizedBaseCode)
                || "BASE_C".equals(normalizedBaseCode)
                || "FUEL".equals(normalizedBaseTypeCode)
                || "C".equals(normalizedBaseTypeCode)) {
            if (maintenanceCapacity != 0) {
                throw new IllegalArgumentException("Base C cannot have repair slots");
            }
            return 0;
        }
        return maintenanceCapacity;
    }

    private void validateScenarioName(String name) {
        if (!name.matches(SCENARIO_NAME_PATTERN)) {
            throw new IllegalArgumentException("Scenario name must contain only uppercase letters, digits, and underscores");
        }
    }
}
