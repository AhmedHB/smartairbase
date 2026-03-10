package se.smartairbase.mcpserver.repository;

import se.smartairbase.mcpserver.domain.rule.AircraftType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AircraftTypeRepository extends JpaRepository<AircraftType, Long> {

    Optional<AircraftType> findByCode(String code);

}