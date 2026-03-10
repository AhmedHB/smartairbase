package se.smartairbase.mcpclient.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import se.smartairbase.mcpclient.domain.SmartAirBaseTool;
import se.smartairbase.mcpclient.controller.dto.AssignMissionRequest;
import se.smartairbase.mcpclient.controller.dto.CreateGameRequest;
import se.smartairbase.mcpclient.controller.dto.DiceRollRequest;
import se.smartairbase.mcpclient.controller.dto.LandAircraftRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmartAirBaseMcpClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createGameUsesCreateGameToolAndRequestBody() throws Exception {
        McpToolExecutor executor = mock(McpToolExecutor.class);
        JsonNode response = objectMapper.readTree("{\"gameId\":1}");
        when(executor.execute(eq(SmartAirBaseTool.CREATE_GAME), eq(new CreateGameRequest("smartairbase", "7"))))
                .thenReturn(response);

        SmartAirBaseMcpClient client = new SmartAirBaseMcpClient(executor);

        JsonNode result = client.createGame(new CreateGameRequest("smartairbase", "7"));

        assertThat(result.get("gameId").asInt()).isEqualTo(1);
        verify(executor).execute(eq(SmartAirBaseTool.CREATE_GAME), eq(new CreateGameRequest("smartairbase", "7")));
    }

    @Test
    void assignMissionBuildsExpectedPayload() throws Exception {
        McpToolExecutor executor = mock(McpToolExecutor.class);
        JsonNode response = objectMapper.readTree("{\"success\":true}");
        when(executor.execute(eq(SmartAirBaseTool.ASSIGN_MISSION), eq(Map.of(
                "gameId", "42",
                "aircraftCode", "F1",
                "missionCode", "M3"
        )))).thenReturn(response);

        SmartAirBaseMcpClient client = new SmartAirBaseMcpClient(executor);

        client.assignMission("42", new AssignMissionRequest("F1", "M3"));

        verify(executor).execute(eq(SmartAirBaseTool.ASSIGN_MISSION), eq(Map.of(
                "gameId", "42",
                "aircraftCode", "F1",
                "missionCode", "M3"
        )));
    }

    @Test
    void recordDiceRollBuildsExpectedPayload() {
        McpToolExecutor executor = mock(McpToolExecutor.class);
        when(executor.execute(eq(SmartAirBaseTool.RECORD_DICE_ROLL), org.mockito.ArgumentMatchers.any()))
                .thenReturn(objectMapper.createObjectNode());

        SmartAirBaseMcpClient client = new SmartAirBaseMcpClient(executor);

        client.recordDiceRoll("7", new DiceRollRequest("F2", 6));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(executor).execute(eq(SmartAirBaseTool.RECORD_DICE_ROLL), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "gameId", "7",
                "aircraftCode", "F2",
                "diceValue", 6
        ));
    }

    @Test
    void landAircraftBuildsExpectedPayload() {
        McpToolExecutor executor = mock(McpToolExecutor.class);
        when(executor.execute(eq(SmartAirBaseTool.LAND_AIRCRAFT), org.mockito.ArgumentMatchers.any()))
                .thenReturn(objectMapper.createObjectNode());

        SmartAirBaseMcpClient client = new SmartAirBaseMcpClient(executor);

        client.landAircraft("9", new LandAircraftRequest("F3", "B"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(executor).execute(eq(SmartAirBaseTool.LAND_AIRCRAFT), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).containsExactlyInAnyOrderEntriesOf(Map.of(
                "gameId", "9",
                "aircraftCode", "F3",
                "baseCode", "B"
        ));
    }
}
