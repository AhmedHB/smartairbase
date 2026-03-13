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

import se.smartairbase.mcpserver.domain.rule.enums.ResourceType;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final se.smartairbase.mcpserver.repository.AircraftTypeRepository aircraftTypeRepository;

    public ScenarioService(ScenarioRepository scenarioRepository,
                           ScenarioBaseRepository scenarioBaseRepository,
                           ScenarioAircraftRepository scenarioAircraftRepository,
                           ScenarioMissionRepository scenarioMissionRepository,
                           ScenarioSupplyRuleRepository scenarioSupplyRuleRepository,
                           RepairRuleRepository repairRuleRepository,
                           GameService gameService,
                           se.smartairbase.mcpserver.repository.AircraftTypeRepository aircraftTypeRepository) {
        this.scenarioRepository = scenarioRepository;
        this.scenarioBaseRepository = scenarioBaseRepository;
        this.scenarioAircraftRepository = scenarioAircraftRepository;
        this.scenarioMissionRepository = scenarioMissionRepository;
        this.scenarioSupplyRuleRepository = scenarioSupplyRuleRepository;
        this.repairRuleRepository = repairRuleRepository;
        this.gameService = gameService;
        this.aircraftTypeRepository = aircraftTypeRepository;
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
        // Custom scenarios may tune base capacities, base start/max inventories,
        // aircraft setup, mission costs, and delivery amounts, while fixed rules
        // such as delivery frequency and system scenarios remain locked.
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
            boolean fuelOutpostBase = isFuelOutpostBase(baseUpdate.code(), baseUpdate.baseTypeCode());
            int maintenanceCapacity = sanitizeBaseMaintenanceCapacity(baseUpdate.code(), baseUpdate.baseTypeCode(), baseUpdate.maintenanceCapacity());
            base.updateCapacity(
                    sanitizeNonNegative(baseUpdate.parkingCapacity(), "parkingCapacity"),
                    maintenanceCapacity
            );
            int fuelMax = sanitizeResourceMax(baseUpdate.fuelMax(), "fuel");
            int weaponsMax = fuelOutpostBase ? 0 : sanitizeResourceMax(baseUpdate.weaponsMax(), "weapons");
            int sparePartsMax = fuelOutpostBase ? 0 : sanitizeResourceMax(baseUpdate.sparePartsMax(), "spareParts");
            base.updateResources(
                    sanitizeResourceStart(baseUpdate.fuelStart(), fuelMax, "fuel"),
                    fuelMax,
                    fuelOutpostBase ? 0 : sanitizeResourceStart(baseUpdate.weaponsStart(), weaponsMax, "weapons"),
                    weaponsMax,
                    fuelOutpostBase ? 0 : sanitizeResourceStart(baseUpdate.sparePartsStart(), sparePartsMax, "spareParts"),
                    sparePartsMax
            );

            Map<ResourceType, ScenarioSupplyRule> supplyRulesByResource = new EnumMap<>(ResourceType.class);
            for (ScenarioSupplyRule rule : scenarioSupplyRuleRepository.findByScenarioBase_Id(base.getId())) {
                supplyRulesByResource.put(rule.getResource(), rule);
            }
            for (ScenarioSupplyRuleDto supplyRuleUpdate : baseUpdate.supplyRules() == null ? List.<ScenarioSupplyRuleDto>of() : baseUpdate.supplyRules()) {
                ResourceType resource = parseResourceType(supplyRuleUpdate.resource());
                ScenarioSupplyRule supplyRule = supplyRulesByResource.get(resource);
                if (supplyRule == null) {
                    throw new IllegalArgumentException("Unknown supply rule for base " + baseUpdate.code() + ": " + supplyRuleUpdate.resource());
                }
                int deliveryAmount = sanitizeNonNegative(supplyRuleUpdate.deliveryAmount(), "deliveryAmount");
                // Base C remains fuel-only even for editable custom scenarios.
                supplyRule.updateDeliveryAmount(fuelOutpostBase && resource != ResourceType.FUEL ? 0 : deliveryAmount);
            }
        });

        // Scenario aircraft are stored one row per starting aircraft, so an edited
        // initial aircraft count arrives as a replaced scenario-aircraft list.
        List<ScenarioAircraftDto> aircraftUpdates = request.aircraft() == null ? List.of() : request.aircraft();
        int totalParkingCapacity = basesByCode.values().stream().mapToInt(ScenarioBase::getParkingCapacity).sum();
        if (aircraftUpdates.isEmpty()) {
            throw new IllegalArgumentException("Aircraft count must be at least 1");
        }
        if (aircraftUpdates.size() > totalParkingCapacity) {
            throw new IllegalArgumentException("Aircraft count exceeds total parking capacity of " + totalParkingCapacity);
        }
        Set<String> aircraftCodes = new HashSet<>();
        scenarioAircraftRepository.deleteAll(scenarioAircraftRepository.findByScenario_Id(scenarioId));
        for (ScenarioAircraftDto aircraftUpdate : aircraftUpdates) {
            if (!aircraftCodes.add(aircraftUpdate.code())) {
                throw new IllegalArgumentException("Duplicate aircraft code in scenario update: " + aircraftUpdate.code());
            }
            if (aircraftUpdate.startBaseCode() != null && !basesByCode.containsKey(aircraftUpdate.startBaseCode())) {
                throw new IllegalArgumentException("Unknown start base for aircraft " + aircraftUpdate.code() + ": " + aircraftUpdate.startBaseCode());
            }
            scenarioAircraftRepository.save(new ScenarioAircraft(
                    scenario,
                    aircraftUpdate.code(),
                    aircraftTypeRepository.findByCode(aircraftUpdate.aircraftTypeCode())
                            .orElseThrow(() -> new IllegalArgumentException("Unknown aircraft type in scenario update: " + aircraftUpdate.aircraftTypeCode())),
                    aircraftUpdate.startBaseCode(),
                    sanitizeNonNegative(aircraftUpdate.fuelStart(), "fuelStart"),
                    sanitizeNonNegative(aircraftUpdate.weaponsStart(), "weaponsStart"),
                    sanitizeNonNegative(aircraftUpdate.flightHoursStart(), "flightHoursStart")
            ));
        }

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

    private int sanitizeResourceMax(Integer value, String resourceName) {
        return sanitizeNonNegative(value, resourceName + "Max");
    }

    private int sanitizeResourceStart(Integer startValue, int maxValue, String resourceName) {
        int sanitizedStart = sanitizeNonNegative(startValue, resourceName + "Start");
        if (sanitizedStart > maxValue) {
            throw new IllegalArgumentException(resourceName + "Start cannot exceed " + resourceName + "Max");
        }
        return sanitizedStart;
    }

    private ResourceType parseResourceType(String value) {
        try {
            return ResourceType.valueOf(value == null ? "" : value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown resource type: " + value);
        }
    }

    private int sanitizeBaseMaintenanceCapacity(String baseCode, String baseTypeCode, Integer value) {
        int maintenanceCapacity = sanitizeNonNegative(value, "maintenanceCapacity");
        if (isFuelOutpostBase(baseCode, baseTypeCode)) {
            if (maintenanceCapacity != 0) {
                throw new IllegalArgumentException("Base C cannot have repair slots");
            }
            return 0;
        }
        return maintenanceCapacity;
    }

    private boolean isFuelOutpostBase(String baseCode, String baseTypeCode) {
        String normalizedBaseCode = baseCode == null ? "" : baseCode.trim().toUpperCase();
        String normalizedBaseTypeCode = baseTypeCode == null ? "" : baseTypeCode.trim().toUpperCase();
        return "C".equals(normalizedBaseCode)
                || "BASE_C".equals(normalizedBaseCode)
                || "FUEL".equals(normalizedBaseTypeCode)
                || "C".equals(normalizedBaseTypeCode);
    }

    private void validateScenarioName(String name) {
        if (!name.matches(SCENARIO_NAME_PATTERN)) {
            throw new IllegalArgumentException("Scenario name must contain only uppercase letters, digits, and underscores");
        }
    }
}
