package se.smartairbase.mcpclient.service;

import org.springframework.stereotype.Service;
import se.smartairbase.mcpclient.domain.BaseReference;
import se.smartairbase.mcpclient.domain.DeliveryScheduleReference;
import se.smartairbase.mcpclient.domain.DiceOutcomeReference;
import se.smartairbase.mcpclient.domain.GameRulesReference;
import se.smartairbase.mcpclient.domain.InitialAircraftReference;
import se.smartairbase.mcpclient.domain.InitialSetupReference;
import se.smartairbase.mcpclient.domain.InventoryReference;
import se.smartairbase.mcpclient.domain.MissionReference;
import se.smartairbase.mcpclient.domain.ResourceRulesReference;

import java.util.List;
import java.util.Map;

@Service
/**
 * Exposes a client-side reference view of the game rules from version 7.
 *
 * <p>The MCP server remains authoritative for validation. This service is only
 * used to present a readable rules summary in the client UI.</p>
 */
public class GameRulesReferenceService {

    /**
     * Returns static reference data derived from the supplied rules document.
     */
    public GameRulesReference getRules() {
        return new GameRulesReference(
                "7",
                List.of(
                        "Complete all missions in as few rounds as possible.",
                        "Avoid losing all aircraft.",
                        "Avoid reaching a state where remaining missions can no longer be flown."
                ),
                new InitialSetupReference(
                        3,
                        List.of("A", "B", "C"),
                        3,
                        100,
                        6,
                        20,
                        List.of(
                                new InitialAircraftReference("F1", "A", 100, 6, 20),
                                new InitialAircraftReference("F2", "A", 100, 6, 20),
                                new InitialAircraftReference("F3", "A", 100, 6, 20)
                        )
                ),
                List.of(
                        new MissionReference("M1", "Recon", 4, 20, 0),
                        new MissionReference("M2", "Strike", 6, 30, 2),
                        new MissionReference("M3", "Deep Strike", 8, 40, 4)
                ),
                List.of(
                        new BaseReference("A", "Main Airbase", 4, 2,
                                new InventoryReference(300, 20, 10),
                                new InventoryReference(500, 40, 20),
                                List.of("refuel", "rearm", "repair", "full service")),
                        new BaseReference("B", "Forward Base", 2, 1,
                                new InventoryReference(200, 10, 4),
                                new InventoryReference(300, 20, 10),
                                List.of("refuel", "rearm", "light repair")),
                        new BaseReference("C", "Fuel Outpost", 2, 0,
                                new InventoryReference(150, 0, 0),
                                new InventoryReference(200, 0, 0),
                                List.of("refuel"))
                ),
                List.of(
                        new DiceOutcomeReference(1, "No fault", 0, 0),
                        new DiceOutcomeReference(2, "Minor repair", 1, 1),
                        new DiceOutcomeReference(3, "Minor repair", 1, 1),
                        new DiceOutcomeReference(4, "Component damage", 2, 2),
                        new DiceOutcomeReference(5, "Major repair", 3, 3),
                        new DiceOutcomeReference(6, "Full service required", 4, 4)
                ),
                new ResourceRulesReference(
                        5,
                        new DeliveryScheduleReference(2, Map.of("A", 50, "B", 40, "C", 30)),
                        new DeliveryScheduleReference(3, Map.of("A", 3, "B", 2, "C", 0)),
                        new DeliveryScheduleReference(4, Map.of("A", 6, "B", 4, "C", 0))
                ),
                List.of(
                        "1. Planning",
                        "2. Resource check",
                        "3. Missions",
                        "4. Dice",
                        "5. Landing and maintenance",
                        "6. Resource update"
                )
        );
    }
}
