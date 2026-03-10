package se.smartairbase.mcpclient.domain;

public record DiceOutcomeReference(
        int diceValue,
        String outcome,
        int sparePartsCost,
        int repairRounds
) {
}
