package se.smartairbase.mcpserver.domain.rule;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "base_type")
/**
 * Seeded base category that defines service capabilities shared by many bases.
 */
public class BaseType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "base_type_id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @OneToMany(mappedBy = "baseType", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BaseTypeService> services = new HashSet<>();

    protected BaseType() {
    }

    public BaseType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public Set<BaseTypeService> getServices() { return services; }
}
