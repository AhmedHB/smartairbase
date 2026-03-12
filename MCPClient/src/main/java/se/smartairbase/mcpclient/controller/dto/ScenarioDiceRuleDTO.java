package se.smartairbase.mcpclient.controller.dto;

public record ScenarioDiceRuleDTO(Integer diceValue,
                                  String damageType,
                                  Integer sparePartsCost,
                                  Integer repairRounds,
                                  boolean requiresFullService) {
}
