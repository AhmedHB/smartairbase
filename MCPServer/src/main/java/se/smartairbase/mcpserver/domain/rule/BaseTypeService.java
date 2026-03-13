package se.smartairbase.mcpserver.domain.rule;

import jakarta.persistence.*;
import se.smartairbase.mcpserver.domain.rule.enums.BaseServiceType;

@Entity
@Table(name = "base_type_service")
/**
 * Join entity that grants one service capability to one base type.
 */
public class BaseTypeService {

    @EmbeddedId
    private BaseTypeServiceId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("baseTypeId")
    @JoinColumn(name = "base_type_id", nullable = false)
    private BaseType baseType;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, insertable = false, updatable = false)
    private BaseServiceType serviceType;

    protected BaseTypeService() {
    }

    public BaseTypeService(BaseType baseType, BaseServiceType serviceType) {
        this.baseType = baseType;
        this.serviceType = serviceType;
        this.id = new BaseTypeServiceId(baseType.getId(), serviceType.name());
    }

    @PrePersist
    @PreUpdate
    private void syncId() {
        if (baseType != null && serviceType != null) {
            this.id = new BaseTypeServiceId(baseType.getId(), serviceType.name());
        }
    }

    public BaseTypeServiceId getId() { return id; }
    public BaseType getBaseType() { return baseType; }
    public BaseServiceType getServiceType() { return serviceType; }
}
