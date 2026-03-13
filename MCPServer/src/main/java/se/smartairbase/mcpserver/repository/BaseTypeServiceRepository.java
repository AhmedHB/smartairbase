package se.smartairbase.mcpserver.repository;


import se.smartairbase.mcpserver.domain.rule.BaseTypeService;
import se.smartairbase.mcpserver.domain.rule.BaseTypeServiceId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import se.smartairbase.mcpserver.domain.rule.enums.BaseServiceType;

/**
 * Loads the service capabilities attached to each base type.
 */
public interface BaseTypeServiceRepository extends JpaRepository<BaseTypeService, BaseTypeServiceId> {

    List<BaseTypeService> findByBaseType_Id(Long baseTypeId);

    boolean existsByBaseType_IdAndServiceType(Long baseTypeId, BaseServiceType serviceType);

}
