package se.smartairbase.mcpclient.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import se.smartairbase.mcpclient.controller.dto.ActionResultDTO;
import se.smartairbase.mcpclient.controller.dto.AnalysisFeedItemDTO;
import se.smartairbase.mcpclient.controller.dto.AnalysisFeedResponseDTO;
import se.smartairbase.mcpclient.controller.dto.AssignMissionRequestDTO;
import se.smartairbase.mcpclient.controller.dto.CreateScenarioGameRequestDTO;
import se.smartairbase.mcpclient.controller.dto.CreateGameRequestDTO;
import se.smartairbase.mcpclient.controller.dto.CreateSimulationBatchRequestDTO;
import se.smartairbase.mcpclient.controller.dto.DiceRollRequestDTO;
import se.smartairbase.mcpclient.controller.dto.DuplicateScenarioRequestDTO;
import se.smartairbase.mcpclient.controller.dto.GameSummaryDTO;
import se.smartairbase.mcpclient.controller.dto.GameAnalyticsSnapshotDTO;
import se.smartairbase.mcpclient.controller.dto.LandAircraftRequestDTO;
import se.smartairbase.mcpclient.controller.dto.ScenarioAircraftDTO;
import se.smartairbase.mcpclient.controller.dto.ScenarioBaseDTO;
import se.smartairbase.mcpclient.controller.dto.ScenarioDefinitionDTO;
import se.smartairbase.mcpclient.controller.dto.ScenarioMissionDTO;
import se.smartairbase.mcpclient.controller.dto.ScenarioSummaryDTO;
import se.smartairbase.mcpclient.controller.dto.SimulationBatchDTO;
import se.smartairbase.mcpclient.controller.dto.UpdateScenarioRequestDTO;
import se.smartairbase.mcpclient.domain.SmartAirBaseTool;

import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmartAirBaseMcpClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createGameUsesCreateGameToolAndRequestBody() {
        McpToolExecutor executor = mock(McpToolExecutor.class);
        GameSummaryDTO response = new GameSummaryDTO(1L, "smartairbase-v7", "smartairbase", "7", "ACTIVE", 0, null, false, true, false, 1000);
        CreateGameRequestDTO request = new CreateGameRequestDTO("smartairbase", "7", "GAME_001", 3, Map.of("M1", 1, "M2", 1, "M3", 1), 1000);
        when(executor.execute(eq(SmartAirBaseTool.CREATE_GAME), eq(request), eq(GameSummaryDTO.class)))
                .thenReturn(response);

        SmartAirBaseMcpClient client = new SmartAirBaseMcpClient(executor, objectMapper);

        GameSummaryDTO result = client.createGame(request);

        assertThat(result.gameId()).isEqualTo(1L);
        verify(executor).execute(eq(SmartAirBaseTool.CREATE_GAME), eq(request), eq(GameSummaryDTO.class));
    }

    @Test
    void createSimulationBatchUsesExpectedToolPayload() {
        McpToolExecutor executor = mock(McpToolExecutor.class);
        CreateSimulationBatchRequestDTO request = new CreateSimulationBatchRequestDTO(
                "SIM_BATCH",
                "SCN_STANDARD",
                3,
                Map.of("M1", 1, "M2", 1, "M3", 1),
                "RANDOM",
                10,
                1000
        );
        SimulationBatchDTO response = new SimulationBatchDTO(
                41L, "SIM_BATCH", "SCN_STANDARD", 3, 1, 1, 1, "RANDOM", 1000, 10, 0, 0, 0, 0, "PENDING", null
        );
        when(executor.execute(eq(SmartAirBaseTool.CREATE_SIMULATION_BATCH), eq(request), eq(SimulationBatchDTO.class)))
                .thenReturn(response);

        SmartAirBaseMcpClient client = new SmartAirBaseMcpClient(executor, objectMapper);

        SimulationBatchDTO result = client.createSimulationBatch(request);

        assertThat(result.simulationBatchId()).isEqualTo(41L);
        verify(executor).execute(eq(SmartAirBaseTool.CREATE_SIMULATION_BATCH), eq(request), eq(SimulationBatchDTO.class));
    }

    @Test
    void getSimulationBatchUsesExpectedToolPayload() {
        McpToolExecutor executor = mock(McpToolExecutor.class);
        when(executor.execute(eq(SmartAirBaseTool.GET_SIMULATION_BATCH), eq(Map.of("simulationBatchId", "41")), eq(SimulationBatchDTO.class)))
                .thenReturn(new SimulationBatchDTO(
                        41L, "SIM_BATCH", "SCN_STANDARD", 3, 1, 1, 1, "RANDOM", 1000, 10, 5, 0, 3, 2, "RUNNING", "SIM_BATCH_006"
                ));

        SmartAirBaseMcpClient client = new SmartAirBaseMcpClient(executor, objectMapper);

        SimulationBatchDTO result = client.getSimulationBatch("41");

        assertThat(result.status()).isEqualTo("RUNNING");
        verify(executor).execute(eq(SmartAirBaseTool.GET_SIMULATION_BATCH), eq(Map.of("simulationBatchId", "41")), eq(SimulationBatchDTO.class));
    }

    @Test
    void listGameAnalyticsSnapshotsUsesExpectedToolPayload() {
        McpToolExecutor executor = mock(McpToolExecutor.class);
        when(executor.execute(eq(SmartAirBaseTool.LIST_GAME_ANALYTICS_SNAPSHOTS), anyMap(), eq(Object.class)))
                .thenReturn(List.of(Map.ofEntries(
                        Map.entry("gameAnalyticsSnapshotId", 7),
                        Map.entry("gameId", 11),
                        Map.entry("gameName", "GAME_001"),
                        Map.entry("scenarioName", "SCN_STANDARD"),
                        Map.entry("gameStatus", "WON"),
                        Map.entry("isWin", true),
                        Map.entry("roundsToOutcome", 2),
                        Map.entry("diceSelectionProfile", "AUTO_RANDOM"),
                        Map.entry("aircraftCount", 3),
                        Map.entry("survivingAircraftCount", 3),
                        Map.entry("destroyedAircraftCount", 0),
                        Map.entry("missionCount", 3),
                        Map.entry("completedMissionCount", 3),
                        Map.entry("m1Count", 1),
                        Map.entry("m2Count", 1),
                        Map.entry("m3Count", 1),
                        Map.entry("createdAt", "2026-03-13T10:00:00")
                )));

        SmartAirBaseMcpClient client = new SmartAirBaseMcpClient(executor, objectMapper);

        List<GameAnalyticsSnapshotDTO> result = client.listGameAnalyticsSnapshots("SCN_STANDARD", "2026-03-13", 3, 1, 1, 1);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().gameName()).isEqualTo("GAME_001");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(executor).execute(eq(SmartAirBaseTool.LIST_GAME_ANALYTICS_SNAPSHOTS), payloadCaptor.capture(), eq(Object.class));
        assertThat(payloadCaptor.getValue()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "scenarioName", "SCN_STANDARD",
                "createdDate", "2026-03-13",
                "aircraftCount", 3,
                "m1Count", 1,
                "m2Count", 1,
                "m3Count", 1
        ));
    }

    @Test
    void listScenariosUsesExpectedToolPayload() {
        McpToolExecutor executor = mock(McpToolExecutor.class);
        when(executor.execute(eq(SmartAirBaseTool.LIST_SCENARIOS), eq(Map.of()), eq(Object.class)))
                .thenReturn(List.of(Map.of(
                        "scenarioId", 1,
                        "name", "Standard Scenario",
                        "version", "V7",
                        "sourceType", "SYSTEM",
                        "editable", false,
                        "deletable", false,
                        "published", true
                )));

        SmartAirBaseMcpClient client = new SmartAirBaseMcpClient(executor, objectMapper);

        List<ScenarioSummaryDTO> result = client.listScenarios();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("Standard Scenario");
        verify(executor).execute(eq(SmartAirBaseTool.LIST_SCENARIOS), eq(Map.of()), eq(Object.class));
    }

    @Test
    void getScenarioUsesExpectedToolPayload() {
        McpToolExecutor executor = mock(McpToolExecutor.class);
        when(executor.execute(eq(SmartAirBaseTool.GET_SCENARIO), eq(Map.of("scenarioId", "1")), eq(ScenarioDefinitionDTO.class)))
                .thenReturn(new ScenarioDefinitionDTO(1L, "Standard Scenario", "V7", "desc", "SYSTEM", false, false, true,
                        List.of(), List.of(), List.of(), List.of()));

        SmartAirBaseMcpClient client = new SmartAirBaseMcpClient(executor, objectMapper);

        ScenarioDefinitionDTO result = client.getScenario("1");

        assertThat(result.name()).isEqualTo("Standard Scenario");
        verify(executor).execute(eq(SmartAirBaseTool.GET_SCENARIO), eq(Map.of("scenarioId", "1")), eq(ScenarioDefinitionDTO.class));
    }

    @Test
    void duplicateScenarioBuildsExpectedPayload() {
        McpToolExecutor executor = mock(McpToolExecutor.class);
        DuplicateScenarioRequestDTO request = new DuplicateScenarioRequestDTO("STANDARD_SCENARIO_COPY");
        when(executor.execute(eq(SmartAirBaseTool.DUPLICATE_SCENARIO), anyMap(), eq(ScenarioDefinitionDTO.class)))
                .thenReturn(new ScenarioDefinitionDTO(2L, "STANDARD_SCENARIO_COPY", "V7", "desc", "USER", true, true, false,
                        List.of(), List.of(), List.of(), List.of()));

        SmartAirBaseMcpClient client = new SmartAirBaseMcpClient(executor, objectMapper);

        client.duplicateScenario("1", request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(executor).execute(eq(SmartAirBaseTool.DUPLICATE_SCENARIO), payloadCaptor.capture(), eq(ScenarioDefinitionDTO.class));
        assertThat(payloadCaptor.getValue()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "scenarioId", "1",
                "name", "STANDARD_SCENARIO_COPY"
        ));
    }

    @Test
    void createGameFromScenarioBuildsExpectedPayload() {
        McpToolExecutor executor = mock(McpToolExecutor.class);
        CreateScenarioGameRequestDTO request = new CreateScenarioGameRequestDTO("Scenario test", 4, Map.of("M1", 2), 250);
        when(executor.execute(eq(SmartAirBaseTool.CREATE_GAME_FROM_SCENARIO), anyMap(), eq(GameSummaryDTO.class)))
                .thenReturn(new GameSummaryDTO(11L, "Scenario test", "SCN_STANDARD", "V7", "ACTIVE", 0, null, false, true, false, 1000));

        SmartAirBaseMcpClient client = new SmartAirBaseMcpClient(executor, objectMapper);

        client.createGameFromScenario("5", request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(executor).execute(eq(SmartAirBaseTool.CREATE_GAME_FROM_SCENARIO), payloadCaptor.capture(), eq(GameSummaryDTO.class));
        assertThat(payloadCaptor.getValue()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "scenarioId", "5",
                "gameName", "Scenario test",
                "aircraftCount", 4,
                "missionTypeCounts", Map.of("M1", 2),
                "maxRounds", 250
        ));
    }

    @Test
    void updateScenarioBuildsExpectedPayload() {
        McpToolExecutor executor = mock(McpToolExecutor.class);
        UpdateScenarioRequestDTO request = new UpdateScenarioRequestDTO(
                "Updated description",
                List.of(new ScenarioBaseDTO("A", "Base A", "MAIN", 5, 3, 100, 100, 0, 0, 0, 0, List.of())),
                List.of(new ScenarioAircraftDTO("F1", "JAS39", "A", 90, 4, 18)),
                List.of(new ScenarioMissionDTO("M1", "M1", "Recon", 1, 1, 25, 1, 5))
        );
        when(executor.execute(eq(SmartAirBaseTool.UPDATE_SCENARIO), anyMap(), eq(ScenarioDefinitionDTO.class)))
                .thenReturn(new ScenarioDefinitionDTO(2L, "WINTER_OPS", "V7", "desc", "USER", true, true, false,
                        request.bases(), request.aircraft(), request.missions(), List.of()));

        SmartAirBaseMcpClient client = new SmartAirBaseMcpClient(executor, objectMapper);

        client.updateScenario("2", request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(executor).execute(eq(SmartAirBaseTool.UPDATE_SCENARIO), payloadCaptor.capture(), eq(ScenarioDefinitionDTO.class));
        assertThat(payloadCaptor.getValue()).containsEntry("scenarioId", "2");
        assertThat(payloadCaptor.getValue()).containsEntry("description", "Updated description");
        assertThat(payloadCaptor.getValue()).containsEntry("bases", request.bases());
        assertThat(payloadCaptor.getValue()).containsEntry("aircraft", request.aircraft());
        assertThat(payloadCaptor.getValue()).containsEntry("missions", request.missions());
    }

    @Test
    void deleteScenarioBuildsExpectedPayload() {
        McpToolExecutor executor = mock(McpToolExecutor.class);
        when(executor.execute(eq(SmartAirBaseTool.DELETE_SCENARIO), eq(Map.of("scenarioId", "7")), eq(ActionResultDTO.class)))
                .thenReturn(new ActionResultDTO(true, "Scenario deleted"));

        SmartAirBaseMcpClient client = new SmartAirBaseMcpClient(executor, objectMapper);

        ActionResultDTO result = client.deleteScenario("7");

        assertThat(result.success()).isTrue();
        verify(executor).execute(eq(SmartAirBaseTool.DELETE_SCENARIO), eq(Map.of("scenarioId", "7")), eq(ActionResultDTO.class));
    }

    @Test
    void assignMissionBuildsExpectedPayload() {
        McpToolExecutor executor = mock(McpToolExecutor.class);
        ActionResultDTO response = new ActionResultDTO(true, "ok");
        when(executor.execute(eq(SmartAirBaseTool.ASSIGN_MISSION), eq(Map.of(
                "gameId", "42",
                "aircraftCode", "F1",
                "missionCode", "M3"
        )), eq(ActionResultDTO.class))).thenReturn(response);

        SmartAirBaseMcpClient client = new SmartAirBaseMcpClient(executor, objectMapper);

        client.assignMission("42", new AssignMissionRequestDTO("F1", "M3"));

        verify(executor).execute(eq(SmartAirBaseTool.ASSIGN_MISSION), eq(Map.of(
                "gameId", "42",
                "aircraftCode", "F1",
                "missionCode", "M3"
        )), eq(ActionResultDTO.class));
    }

    @Test
    void recordDiceRollBuildsExpectedPayload() {
        McpToolExecutor executor = mock(McpToolExecutor.class);
        when(executor.execute(eq(SmartAirBaseTool.RECORD_DICE_ROLL), anyMap(), eq(ActionResultDTO.class)))
                .thenReturn(new ActionResultDTO(true, "ok"));

        SmartAirBaseMcpClient client = new SmartAirBaseMcpClient(executor, objectMapper);

        client.recordDiceRoll("7", new DiceRollRequestDTO("F2", 6, "AUTO_RANDOM"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(executor).execute(eq(SmartAirBaseTool.RECORD_DICE_ROLL), payloadCaptor.capture(), eq(ActionResultDTO.class));
        assertThat(payloadCaptor.getValue()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "gameId", "7",
                "aircraftCode", "F2",
                "diceValue", 6,
                "diceSelectionMode", "AUTO_RANDOM"
        ));
    }

    @Test
    void abortGameBuildsExpectedPayload() {
        McpToolExecutor executor = mock(McpToolExecutor.class);
        when(executor.execute(eq(SmartAirBaseTool.ABORT_GAME), eq(Map.of("gameId", "9")), eq(ActionResultDTO.class)))
                .thenReturn(new ActionResultDTO(true, "Game aborted"));

        SmartAirBaseMcpClient client = new SmartAirBaseMcpClient(executor, objectMapper);

        ActionResultDTO result = client.abortGame("9");

        assertThat(result.success()).isTrue();
        verify(executor).execute(eq(SmartAirBaseTool.ABORT_GAME), eq(Map.of("gameId", "9")), eq(ActionResultDTO.class));
    }

    @Test
    void landAircraftBuildsExpectedPayload() {
        McpToolExecutor executor = mock(McpToolExecutor.class);
        when(executor.execute(eq(SmartAirBaseTool.LAND_AIRCRAFT), anyMap(), eq(ActionResultDTO.class)))
                .thenReturn(new ActionResultDTO(true, "ok"));

        SmartAirBaseMcpClient client = new SmartAirBaseMcpClient(executor, objectMapper);

        client.landAircraft("9", new LandAircraftRequestDTO("F3", "B"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(executor).execute(eq(SmartAirBaseTool.LAND_AIRCRAFT), payloadCaptor.capture(), eq(ActionResultDTO.class));
        assertThat(payloadCaptor.getValue()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "gameId", "9",
                "aircraftCode", "F3",
                "baseCode", "B"
        ));
    }

    @Test
    void listAnalysisFeedUsesExpectedToolPayload() {
        McpToolExecutor executor = mock(McpToolExecutor.class);
        AnalysisFeedResponseDTO response = new AnalysisFeedResponseDTO(List.of(), false, 2);
        when(executor.execute(eq(SmartAirBaseTool.LIST_ANALYSIS_FEED), eq(Map.of("gameId", "9")), eq(AnalysisFeedResponseDTO.class)))
                .thenReturn(response);

        SmartAirBaseMcpClient client = new SmartAirBaseMcpClient(executor, objectMapper);

        AnalysisFeedResponseDTO result = client.listAnalysisFeed("9");

        assertThat(result.lastAnalyzedRound()).isEqualTo(2);
        verify(executor).execute(eq(SmartAirBaseTool.LIST_ANALYSIS_FEED), eq(Map.of("gameId", "9")), eq(AnalysisFeedResponseDTO.class));
    }

    @Test
    void appendAnalysisFeedItemsBuildsExpectedPayload() {
        McpToolExecutor executor = mock(McpToolExecutor.class);
        when(executor.execute(eq(SmartAirBaseTool.APPEND_ANALYSIS_FEED_ITEMS), anyMap(), eq(AnalysisFeedResponseDTO.class)))
                .thenReturn(new AnalysisFeedResponseDTO(List.of(), false, 3));

        SmartAirBaseMcpClient client = new SmartAirBaseMcpClient(executor, objectMapper);
        List<AnalysisFeedItemDTO> items = List.of(new AnalysisFeedItemDTO(
                "1",
                9L,
                3,
                "ROUND_COMPLETE",
                "Captain Erik Holm (Pilot)",
                "LLM",
                "Summary",
                null,
                List.of("F1"),
                List.of("BASE_A"),
                "2026-03-11T12:00:00Z"
        ));

        client.appendAnalysisFeedItems("9", items);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(executor).execute(eq(SmartAirBaseTool.APPEND_ANALYSIS_FEED_ITEMS), payloadCaptor.capture(), eq(AnalysisFeedResponseDTO.class));
        assertThat(payloadCaptor.getValue()).containsEntry("gameId", "9");
        assertThat(payloadCaptor.getValue()).containsEntry("items", items);
    }
}
