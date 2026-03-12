package se.smartairbase.mcpserver.mcp.tools;

import se.smartairbase.mcpserver.mcp.dto.ScenarioAircraftDto;
import se.smartairbase.mcpserver.mcp.dto.ScenarioBaseDto;
import se.smartairbase.mcpserver.mcp.dto.ScenarioMissionDto;
import se.smartairbase.mcpserver.mcp.dto.UpdateScenarioRequestDto;

import java.util.List;
import java.util.Map;

final class ScenarioToolMapper {

    private ScenarioToolMapper() {
    }

    static UpdateScenarioRequestDto toUpdateScenarioRequest(List<Map<String, Object>> bases,
                                                            List<Map<String, Object>> aircraft,
                                                            List<Map<String, Object>> missions,
                                                            String description) {
        return new UpdateScenarioRequestDto(
                description,
                (bases == null ? List.<Map<String, Object>>of() : bases).stream().map(ScenarioToolMapper::toBase).toList(),
                (aircraft == null ? List.<Map<String, Object>>of() : aircraft).stream().map(ScenarioToolMapper::toAircraft).toList(),
                (missions == null ? List.<Map<String, Object>>of() : missions).stream().map(ScenarioToolMapper::toMission).toList()
        );
    }

    private static ScenarioBaseDto toBase(Map<String, Object> item) {
        return new ScenarioBaseDto(
                stringValue(item.get("code")),
                stringValue(item.get("name")),
                stringValue(item.get("baseTypeCode")),
                intValue(item.get("parkingCapacity")),
                intValue(item.get("maintenanceCapacity")),
                intValue(item.get("fuelStart")),
                intValue(item.get("fuelMax")),
                intValue(item.get("weaponsStart")),
                intValue(item.get("weaponsMax")),
                intValue(item.get("sparePartsStart")),
                intValue(item.get("sparePartsMax")),
                List.of()
        );
    }

    private static ScenarioAircraftDto toAircraft(Map<String, Object> item) {
        return new ScenarioAircraftDto(
                stringValue(item.get("code")),
                stringValue(item.get("aircraftTypeCode")),
                stringValue(item.get("startBaseCode")),
                intValue(item.get("fuelStart")),
                intValue(item.get("weaponsStart")),
                intValue(item.get("flightHoursStart"))
        );
    }

    private static ScenarioMissionDto toMission(Map<String, Object> item) {
        return new ScenarioMissionDto(
                stringValue(item.get("code")),
                stringValue(item.get("missionTypeCode")),
                stringValue(item.get("missionTypeName")),
                intValue(item.get("sortOrder")),
                intValue(item.get("defaultCount")),
                intValue(item.get("fuelCost")),
                intValue(item.get("weaponCost")),
                intValue(item.get("flightTimeCost"))
        );
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Integer intValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
