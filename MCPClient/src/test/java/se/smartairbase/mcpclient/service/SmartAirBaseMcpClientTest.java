package se.smartairbase.mcpclient.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import se.smartairbase.mcpclient.controller.dto.ActionResultDTO;
import se.smartairbase.mcpclient.controller.dto.AssignMissionRequestDTO;
import se.smartairbase.mcpclient.controller.dto.CreateGameRequestDTO;
import se.smartairbase.mcpclient.controller.dto.DiceRollRequestDTO;
import se.smartairbase.mcpclient.controller.dto.GameSummaryDTO;
import se.smartairbase.mcpclient.controller.dto.LandAircraftRequestDTO;
import se.smartairbase.mcpclient.domain.SmartAirBaseTool;

import java.util.Map;

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
        GameSummaryDTO response = new GameSummaryDTO(1L, "smartairbase-v7", "smartairbase", "7", "ACTIVE", 0, null, false, true, false);
        when(executor.execute(eq(SmartAirBaseTool.CREATE_GAME), eq(new CreateGameRequestDTO("smartairbase", "7")), eq(GameSummaryDTO.class)))
                .thenReturn(response);

        SmartAirBaseMcpClient client = new SmartAirBaseMcpClient(executor, objectMapper);

        GameSummaryDTO result = client.createGame(new CreateGameRequestDTO("smartairbase", "7"));

        assertThat(result.gameId()).isEqualTo(1L);
        verify(executor).execute(eq(SmartAirBaseTool.CREATE_GAME), eq(new CreateGameRequestDTO("smartairbase", "7")), eq(GameSummaryDTO.class));
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

        client.recordDiceRoll("7", new DiceRollRequestDTO("F2", 6));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(executor).execute(eq(SmartAirBaseTool.RECORD_DICE_ROLL), payloadCaptor.capture(), eq(ActionResultDTO.class));
        assertThat(payloadCaptor.getValue()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "gameId", "7",
                "aircraftCode", "F2",
                "diceValue", 6
        ));
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
}
