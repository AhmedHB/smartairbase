package se.smartairbase.mcpserver.mcp.dto;

public record ScenarioDiceRuleDto(Integer diceValue,
                                  String damageType,
                                  Integer sparePartsCost,
                                  Integer repairRounds,
                                  boolean requiresFullService) {
}
