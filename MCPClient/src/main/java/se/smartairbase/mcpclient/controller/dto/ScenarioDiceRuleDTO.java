package se.smartairbase.mcpclient.controller.dto;

/**
 * Browser-facing DTO for one dice outcome rule in a scenario overview.
 */
public record ScenarioDiceRuleDTO(Integer diceValue,
                                  String damageType,
                                  Integer sparePartsCost,
                                  Integer repairRounds,
                                  boolean requiresFullService) {
}
