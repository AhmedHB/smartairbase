package se.smartairbase.mcpserver.repository;


import se.smartairbase.mcpserver.domain.rule.BaseTypeService;
import se.smartairbase.mcpserver.domain.rule.BaseTypeServiceId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaseTypeServiceRepository extends JpaRepository<BaseTypeService, BaseTypeServiceId> {

    List<BaseTypeService> findByBaseType_Id(Long baseTypeId);

}
