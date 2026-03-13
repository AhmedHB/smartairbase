package se.smartairbase.mcpserver.mcp.dto;

/**
 * Describes one dice outcome rule in a scenario overview response.
 */
public record ScenarioDiceRuleDto(Integer diceValue,
                                  String damageType,
                                  Integer sparePartsCost,
                                  Integer repairRounds,
                                  boolean requiresFullService) {
}
