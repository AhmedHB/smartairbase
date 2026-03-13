package se.smartairbase.mcpserver.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import se.smartairbase.mcpserver.mcp.dto.GameSummaryDto;
import se.smartairbase.mcpserver.mcp.dto.ActionResultDto;
import se.smartairbase.mcpserver.mcp.dto.ScenarioDefinitionDto;
import se.smartairbase.mcpserver.mcp.dto.ScenarioAircraftDto;
import se.smartairbase.mcpserver.mcp.dto.ScenarioBaseDto;
import se.smartairbase.mcpserver.mcp.dto.ScenarioMissionDto;
import se.smartairbase.mcpserver.mcp.dto.ScenarioSummaryDto;
import se.smartairbase.mcpserver.mcp.dto.ScenarioSupplyRuleDto;
import se.smartairbase.mcpserver.mcp.dto.UpdateScenarioRequestDto;
import se.smartairbase.mcpserver.repository.AircraftStateRepository;
import se.smartairbase.mcpserver.repository.GameAircraftRepository;
import se.smartairbase.mcpserver.repository.GameMissionRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.liquibase.clear-checksums=true"
})
@Transactional
class ScenarioServiceTests {

    @Autowired
    private ScenarioService scenarioService;

    @Autowired
    private GameQueryService gameQueryService;

    @Autowired
    private GameAircraftRepository gameAircraftRepository;

    @Autowired
    private AircraftStateRepository aircraftStateRepository;

    @Autowired
    private GameMissionRepository gameMissionRepository;

    @Test
    void listScenariosIncludesSeededStandardScenario() {
        List<ScenarioSummaryDto> scenarios = scenarioService.listScenarios();

        assertThat(scenarios).isNotEmpty();
        assertThat(scenarios).anyMatch(scenario ->
                "SCN_STANDARD".equals(scenario.name())
                        && "SYSTEM".equals(scenario.sourceType())
                        && !scenario.editable()
                        && !scenario.deletable());
    }

    @Test
    void duplicateScenarioCreatesUserOwnedCopy() {
        Long standardScenarioId = scenarioService.listScenarios().stream()
                .filter(scenario -> "SCN_STANDARD".equals(scenario.name()))
                .findFirst()
                .orElseThrow()
                .scenarioId();

        ScenarioDefinitionDto duplicate = scenarioService.duplicateScenario(standardScenarioId, "SCN_STANDARD_COPY");

        assertThat(duplicate.name()).isEqualTo("SCN_STANDARD_COPY");
        assertThat(duplicate.sourceType()).isEqualTo("USER");
        assertThat(duplicate.editable()).isTrue();
        assertThat(duplicate.deletable()).isTrue();
        assertThat(duplicate.bases()).isNotEmpty();
        assertThat(duplicate.missions()).isNotEmpty();
        assertThat(duplicate.diceRules()).hasSize(6);
    }

    @Test
    void createGameFromScenarioUsesSelectedScenario() {
        Long standardScenarioId = scenarioService.listScenarios().stream()
                .filter(scenario -> "SCN_STANDARD".equals(scenario.name()))
                .findFirst()
                .orElseThrow()
                .scenarioId();

        ScenarioDefinitionDto duplicate = scenarioService.duplicateScenario(standardScenarioId, "SCENARIO_TEST_COPY");
        GameSummaryDto game = scenarioService.createGameFromScenario(duplicate.scenarioId(), "Scenario test game", null, null, null);

        assertThat(game.name()).isEqualTo("Scenario test game");
        assertThat(game.scenarioName()).isEqualTo("SCENARIO_TEST_COPY");
        assertThat(game.status()).isEqualTo("ACTIVE");
        assertThat(gameQueryService.getGameState(game.gameId()).game().scenarioName()).isEqualTo("SCENARIO_TEST_COPY");
    }

    @Test
    void updateScenarioPersistsAircraftMissionAndBaseSettings() {
        Long standardScenarioId = scenarioService.listScenarios().stream()
                .filter(scenario -> "SCN_STANDARD".equals(scenario.name()))
                .findFirst()
                .orElseThrow()
                .scenarioId();

        ScenarioDefinitionDto duplicate = scenarioService.duplicateScenario(standardScenarioId, "EDITABLE_SCENARIO");
        UpdateScenarioRequestDto request = new UpdateScenarioRequestDto(
                "Updated scenario description",
                duplicate.bases().stream()
                        .map(base -> new ScenarioBaseDto(
                                base.code(),
                                base.name(),
                                base.baseTypeCode(),
                                "BASE_A".equals(base.code()) ? 6 : base.parkingCapacity(),
                                "BASE_A".equals(base.code()) ? 3 : base.maintenanceCapacity(),
                                "BASE_A".equals(base.code()) ? 140 : base.fuelStart(),
                                "BASE_A".equals(base.code()) ? 240 : base.fuelMax(),
                                "BASE_A".equals(base.code()) ? 8 : base.weaponsStart(),
                                "BASE_A".equals(base.code()) ? 20 : base.weaponsMax(),
                                "BASE_A".equals(base.code()) ? 3 : base.sparePartsStart(),
                                "BASE_A".equals(base.code()) ? 10 : base.sparePartsMax(),
                                "BASE_A".equals(base.code())
                                        ? base.supplyRules().stream()
                                        .map(rule -> new ScenarioSupplyRuleDto(
                                                rule.resource(),
                                                rule.frequencyRounds(),
                                                "FUEL".equals(rule.resource()) ? 75 : rule.deliveryAmount()
                                        )).toList()
                                        : base.supplyRules()
                        )).toList(),
                duplicate.aircraft().stream()
                        .map(aircraft -> new ScenarioAircraftDto(
                                aircraft.code(),
                                aircraft.aircraftTypeCode(),
                                aircraft.startBaseCode(),
                                "F1".equals(aircraft.code()) ? 88 : aircraft.fuelStart(),
                                "F1".equals(aircraft.code()) ? 4 : aircraft.weaponsStart(),
                                "F1".equals(aircraft.code()) ? 17 : aircraft.flightHoursStart()
                        )).toList(),
                duplicate.missions().stream()
                        .map(mission -> new ScenarioMissionDto(
                                mission.code(),
                                mission.missionTypeCode(),
                                mission.missionTypeName(),
                                mission.sortOrder(),
                                mission.defaultCount(),
                                "M1".equals(mission.code()) ? 24 : mission.fuelCost(),
                                "M1".equals(mission.code()) ? 1 : mission.weaponCost(),
                                "M1".equals(mission.code()) ? 5 : mission.flightTimeCost()
                        )).toList()
        );

        ScenarioDefinitionDto updated = scenarioService.updateScenario(duplicate.scenarioId(), request);
        GameSummaryDto game = scenarioService.createGameFromScenario(updated.scenarioId(), "Edited scenario game", null, null, null);
        var gameState = gameQueryService.getGameState(game.gameId());

        assertThat(updated.bases()).anyMatch(base -> "BASE_A".equals(base.code()) && base.parkingCapacity() == 6 && base.maintenanceCapacity() == 3);
        assertThat(updated.bases()).anyMatch(base -> "BASE_A".equals(base.code())
                && base.fuelStart() == 140
                && base.fuelMax() == 240
                && base.weaponsStart() == 8
                && base.weaponsMax() == 20
                && base.sparePartsStart() == 3
                && base.sparePartsMax() == 10);
        assertThat(updated.bases()).anyMatch(base -> "BASE_A".equals(base.code())
                && base.supplyRules().stream().anyMatch(rule -> "FUEL".equals(rule.resource()) && rule.deliveryAmount() == 75));
        assertThat(updated.aircraft()).anyMatch(aircraft -> "F1".equals(aircraft.code()) && aircraft.fuelStart() == 88 && aircraft.flightHoursStart() == 17);
        assertThat(updated.missions()).anyMatch(mission -> "M1".equals(mission.code()) && mission.fuelCost() == 24 && mission.flightTimeCost() == 5);
        assertThat(updated.description()).isEqualTo("Updated scenario description");
        assertThat(gameState.bases()).anyMatch(base -> "BASE_A".equals(base.code()) && base.parkingCapacity() == 6 && base.maintenanceCapacity() == 3);
        assertThat(gameState.bases()).anyMatch(base -> "BASE_A".equals(base.code())
                && base.fuelStock() == 140
                && base.weaponsStock() == 8
                && base.sparePartsStock() == 3);
        assertThat(gameState.aircraft()).anyMatch(aircraft -> "F1".equals(aircraft.code()) && aircraft.fuel() == 88 && aircraft.remainingFlightHours() == 17);
        assertThat(gameAircraftRepository.findByGame_IdAndCode(game.gameId(), "F1")).isPresent();
        assertThat(aircraftStateRepository.findByGameAircraft_Id(gameAircraftRepository.findByGame_IdAndCode(game.gameId(), "F1").orElseThrow().getId()))
                .get()
                .extracting(state -> state.getWeapons(), state -> state.getFuel())
                .containsExactly(4, 88);
        assertThat(gameMissionRepository.findByGame_IdAndCode(game.gameId(), "M1-1"))
                .get()
                .extracting(mission -> mission.getFuelCost(), mission -> mission.getWeaponCost(), mission -> mission.getFlightTimeCost())
                .containsExactly(24, 1, 5);
    }

    @Test
    void deleteScenarioRemovesUserScenarioButRejectsSystemScenario() {
        Long standardScenarioId = scenarioService.listScenarios().stream()
                .filter(scenario -> "SCN_STANDARD".equals(scenario.name()))
                .findFirst()
                .orElseThrow()
                .scenarioId();

        ScenarioDefinitionDto duplicate = scenarioService.duplicateScenario(standardScenarioId, "DISPOSABLE_SCENARIO");
        ActionResultDto deleteResult = scenarioService.deleteScenario(duplicate.scenarioId());

        assertThat(deleteResult.success()).isTrue();
        assertThat(scenarioService.listScenarios())
                .noneMatch(scenario -> "DISPOSABLE_SCENARIO".equals(scenario.name()));

        assertThatThrownBy(() -> scenarioService.deleteScenario(standardScenarioId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be deleted");
    }

    @Test
    void updateScenarioRejectsNegativeValues() {
        Long standardScenarioId = scenarioService.listScenarios().stream()
                .filter(scenario -> "SCN_STANDARD".equals(scenario.name()))
                .findFirst()
                .orElseThrow()
                .scenarioId();

        ScenarioDefinitionDto duplicate = scenarioService.duplicateScenario(standardScenarioId, "NEGATIVE_VALUE_SCENARIO");
        UpdateScenarioRequestDto request = new UpdateScenarioRequestDto(
                duplicate.description(),
                duplicate.bases(),
                duplicate.aircraft().stream()
                        .map(aircraft -> new ScenarioAircraftDto(
                                aircraft.code(),
                                aircraft.aircraftTypeCode(),
                                aircraft.startBaseCode(),
                                "F1".equals(aircraft.code()) ? -1 : aircraft.fuelStart(),
                                aircraft.weaponsStart(),
                                aircraft.flightHoursStart()
                        )).toList(),
                duplicate.missions()
        );

        assertThatThrownBy(() -> scenarioService.updateScenario(duplicate.scenarioId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fuelStart must be zero or higher");
    }

    @Test
    void updateScenarioForcesBaseCToKeepZeroRepairSlots() {
        Long standardScenarioId = scenarioService.listScenarios().stream()
                .filter(scenario -> "SCN_STANDARD".equals(scenario.name()))
                .findFirst()
                .orElseThrow()
                .scenarioId();

        ScenarioDefinitionDto duplicate = scenarioService.duplicateScenario(standardScenarioId, "BASE_C_RULE_SCENARIO");
        UpdateScenarioRequestDto request = new UpdateScenarioRequestDto(
                duplicate.description(),
                duplicate.bases().stream()
                        .map(base -> new ScenarioBaseDto(
                                base.code(),
                                base.name(),
                                base.baseTypeCode(),
                                base.parkingCapacity(),
                                "BASE_C".equals(base.code()) ? 1 : base.maintenanceCapacity(),
                                base.fuelStart(),
                                base.fuelMax(),
                                base.weaponsStart(),
                                base.weaponsMax(),
                                base.sparePartsStart(),
                                base.sparePartsMax(),
                                base.supplyRules()
                        )).toList(),
                duplicate.aircraft(),
                duplicate.missions()
        );

        assertThatThrownBy(() -> scenarioService.updateScenario(duplicate.scenarioId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Base C cannot have repair slots");
    }

    @Test
    void updateScenarioKeepsBaseCWeaponsAndSparePartsFixedAtZero() {
        Long standardScenarioId = scenarioService.listScenarios().stream()
                .filter(scenario -> "SCN_STANDARD".equals(scenario.name()))
                .findFirst()
                .orElseThrow()
                .scenarioId();

        ScenarioDefinitionDto duplicate = scenarioService.duplicateScenario(standardScenarioId, "BASE_C_FUEL_ONLY_SCENARIO");
        UpdateScenarioRequestDto request = new UpdateScenarioRequestDto(
                duplicate.description(),
                duplicate.bases().stream()
                        .map(base -> {
                            if (!"BASE_C".equals(base.code())) {
                                return base;
                            }
                            return new ScenarioBaseDto(
                                    base.code(),
                                    base.name(),
                                    base.baseTypeCode(),
                                    base.parkingCapacity(),
                                    base.maintenanceCapacity(),
                                    55,
                                    120,
                                    9,
                                    14,
                                    3,
                                    7,
                                    base.supplyRules().stream()
                                            .map(rule -> switch (rule.resource()) {
                                                case "FUEL" -> new ScenarioSupplyRuleDto(rule.resource(), rule.frequencyRounds(), 80);
                                                case "WEAPONS" -> new ScenarioSupplyRuleDto(rule.resource(), rule.frequencyRounds(), 4);
                                                case "SPARE_PARTS" -> new ScenarioSupplyRuleDto(rule.resource(), rule.frequencyRounds(), 2);
                                                default -> rule;
                                            })
                                            .toList()
                            );
                        }).toList(),
                duplicate.aircraft(),
                duplicate.missions()
        );

        ScenarioDefinitionDto updated = scenarioService.updateScenario(duplicate.scenarioId(), request);

        assertThat(updated.bases()).anyMatch(base -> "BASE_C".equals(base.code())
                && base.fuelStart() == 55
                && base.fuelMax() == 120
                && base.weaponsStart() == 0
                && base.weaponsMax() == 0
                && base.sparePartsStart() == 0
                && base.sparePartsMax() == 0
                && base.supplyRules().stream().anyMatch(rule -> "FUEL".equals(rule.resource()) && rule.deliveryAmount() == 80)
                && base.supplyRules().stream()
                .filter(rule -> "WEAPONS".equals(rule.resource()) || "SPARE_PARTS".equals(rule.resource()))
                .allMatch(rule -> rule.deliveryAmount() == 0));
    }

    @Test
    void updateScenarioCanChangeAircraftCountWithinTotalParkingCapacity() {
        Long standardScenarioId = scenarioService.listScenarios().stream()
                .filter(scenario -> "SCN_STANDARD".equals(scenario.name()))
                .findFirst()
                .orElseThrow()
                .scenarioId();

        ScenarioDefinitionDto duplicate = scenarioService.duplicateScenario(standardScenarioId, "AIRCRAFT_COUNT_SCENARIO");
        UpdateScenarioRequestDto request = new UpdateScenarioRequestDto(
                duplicate.description(),
                duplicate.bases(),
                List.of(
                        new ScenarioAircraftDto("F1", "FIGHTER_STD", "BASE_A", 100, 6, 20),
                        new ScenarioAircraftDto("F2", "FIGHTER_STD", "BASE_A", 100, 6, 20),
                        new ScenarioAircraftDto("F3", "FIGHTER_STD", "BASE_A", 100, 6, 20),
                        new ScenarioAircraftDto("F4", "FIGHTER_STD", "BASE_A", 100, 6, 20)
                ),
                duplicate.missions()
        );

        ScenarioDefinitionDto updated = scenarioService.updateScenario(duplicate.scenarioId(), request);

        assertThat(updated.aircraft()).hasSize(4);
        assertThat(updated.aircraft()).anyMatch(aircraft -> "F4".equals(aircraft.code()));
    }

    @Test
    void updateScenarioRejectsAircraftCountAboveTotalParkingCapacity() {
        Long standardScenarioId = scenarioService.listScenarios().stream()
                .filter(scenario -> "SCN_STANDARD".equals(scenario.name()))
                .findFirst()
                .orElseThrow()
                .scenarioId();

        ScenarioDefinitionDto duplicate = scenarioService.duplicateScenario(standardScenarioId, "AIRCRAFT_CAPACITY_LIMIT_SCENARIO");
        UpdateScenarioRequestDto request = new UpdateScenarioRequestDto(
                duplicate.description(),
                duplicate.bases(),
                List.of(
                        new ScenarioAircraftDto("F1", "FIGHTER_STD", "BASE_A", 100, 6, 20),
                        new ScenarioAircraftDto("F2", "FIGHTER_STD", "BASE_A", 100, 6, 20),
                        new ScenarioAircraftDto("F3", "FIGHTER_STD", "BASE_A", 100, 6, 20),
                        new ScenarioAircraftDto("F4", "FIGHTER_STD", "BASE_A", 100, 6, 20),
                        new ScenarioAircraftDto("F5", "FIGHTER_STD", "BASE_A", 100, 6, 20),
                        new ScenarioAircraftDto("F6", "FIGHTER_STD", "BASE_A", 100, 6, 20),
                        new ScenarioAircraftDto("F7", "FIGHTER_STD", "BASE_A", 100, 6, 20),
                        new ScenarioAircraftDto("F8", "FIGHTER_STD", "BASE_A", 100, 6, 20),
                        new ScenarioAircraftDto("F9", "FIGHTER_STD", "BASE_A", 100, 6, 20)
                ),
                duplicate.missions()
        );

        assertThatThrownBy(() -> scenarioService.updateScenario(duplicate.scenarioId(), request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Aircraft count exceeds total parking capacity");
    }
}
