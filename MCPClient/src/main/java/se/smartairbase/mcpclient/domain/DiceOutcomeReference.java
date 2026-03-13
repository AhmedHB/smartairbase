package se.smartairbase.mcpclient.domain;

/**
 * Immutable reference data for one dice outcome in the rules summary.
 */
public record DiceOutcomeReference(
        int diceValue,
        String outcome,
        int sparePartsCost,
        int repairRounds
) {
}
