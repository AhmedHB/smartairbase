package se.smartairbase.mcpserver.domain.rule;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class BaseTypeServiceId implements Serializable {

    @Column(name = "base_type_id")
    private Long baseTypeId;

    @Column(name = "service_type", length = 30)
    private String serviceType;

    protected BaseTypeServiceId() {
    }

    public BaseTypeServiceId(Long baseTypeId, String serviceType) {
        this.baseTypeId = baseTypeId;
        this.serviceType = serviceType;
    }

    public Long getBaseTypeId() { return baseTypeId; }
    public String getServiceType() { return serviceType; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseTypeServiceId that)) return false;
        return Objects.equals(baseTypeId, that.baseTypeId)
                && Objects.equals(serviceType, that.serviceType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseTypeId, serviceType);
    }
}
