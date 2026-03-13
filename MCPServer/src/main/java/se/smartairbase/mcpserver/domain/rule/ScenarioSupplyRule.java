package se.smartairbase.mcpserver.domain.rule;

import jakarta.persistence.*;
import se.smartairbase.mcpserver.domain.rule.enums.ResourceType;

@Entity
@Table(name = "scenario_supply_rule",
        uniqueConstraints = @UniqueConstraint(name = "uk_supply_rule_base_resource", columnNames = {"scenario_base_id", "resource"}))
public class ScenarioSupplyRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scenario_supply_rule_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scenario_base_id", nullable = false)
    private ScenarioBase scenarioBase;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource", nullable = false, length = 30)
    private ResourceType resource;

    @Column(name = "frequency_rounds", nullable = false)
    private Integer frequencyRounds;

    @Column(name = "delivery_amount", nullable = false)
    private Integer deliveryAmount;

    protected ScenarioSupplyRule() {
    }

    public ScenarioSupplyRule(ScenarioBase scenarioBase, ResourceType resource, Integer frequencyRounds, Integer deliveryAmount) {
        this.scenarioBase = scenarioBase;
        this.resource = resource;
        this.frequencyRounds = frequencyRounds;
        this.deliveryAmount = deliveryAmount;
    }

    public void updateDeliveryAmount(Integer deliveryAmount) {
        this.deliveryAmount = deliveryAmount;
    }

    public Long getId() { return id; }
    public ScenarioBase getScenarioBase() { return scenarioBase; }
    public ResourceType getResource() { return resource; }
    public Integer getFrequencyRounds() { return frequencyRounds; }
    public Integer getDeliveryAmount() { return deliveryAmount; }
}
