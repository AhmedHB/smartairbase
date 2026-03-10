package se.smartairbase.mcpserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.smartairbase.mcpserver.domain.game.BaseState;

import java.util.Optional;

public interface BaseStateRepository extends JpaRepository<BaseState, Long> {

    Optional<BaseState> findByGameBase_Id(Long gameBaseId);
}
