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
        GameSummaryDto game = scenarioService.createGameFromScenario(duplicate.scenarioId(), "Scenario test game");

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
                                base.fuelStart(),
                                base.fuelMax(),
                                base.weaponsStart(),
                                base.weaponsMax(),
                                base.sparePartsStart(),
                                base.sparePartsMax(),
                                base.supplyRules()
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
        GameSummaryDto game = scenarioService.createGameFromScenario(updated.scenarioId(), "Edited scenario game");
        var gameState = gameQueryService.getGameState(game.gameId());

        assertThat(updated.bases()).anyMatch(base -> "BASE_A".equals(base.code()) && base.parkingCapacity() == 6 && base.maintenanceCapacity() == 3);
        assertThat(updated.aircraft()).anyMatch(aircraft -> "F1".equals(aircraft.code()) && aircraft.fuelStart() == 88 && aircraft.flightHoursStart() == 17);
        assertThat(updated.missions()).anyMatch(mission -> "M1".equals(mission.code()) && mission.fuelCost() == 24 && mission.flightTimeCost() == 5);
        assertThat(updated.description()).isEqualTo("Updated scenario description");
        assertThat(gameState.bases()).anyMatch(base -> "BASE_A".equals(base.code()) && base.parkingCapacity() == 6 && base.maintenanceCapacity() == 3);
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
}
