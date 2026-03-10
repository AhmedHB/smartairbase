package se.smartairbase.mcpclient.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameRulesReferenceServiceTest {

    @Test
    void exposesVersionSevenReferenceData() {
        GameRulesReferenceService service = new GameRulesReferenceService();

        var rules = service.getRules();

        assertThat(rules.version()).isEqualTo("7");
        assertThat(rules.missions()).hasSize(3);
        assertThat(rules.bases()).extracting("code").containsExactly("A", "B", "C");
        assertThat(rules.resourceRules().holdingFuelCostPerRound()).isEqualTo(5);
    }
}
