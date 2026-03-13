package se.smartairbase.mcpserver.repository;

import se.smartairbase.mcpserver.domain.rule.BaseType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Loads base type rule definitions.
 */
public interface BaseTypeRepository extends JpaRepository<BaseType, Long> {

    Optional<BaseType> findByCode(String code);

}
