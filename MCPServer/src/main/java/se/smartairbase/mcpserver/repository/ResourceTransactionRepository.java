package se.smartairbase.mcpserver.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.smartairbase.mcpserver.domain.game.ResourceTransaction;

import java.util.List;

public interface ResourceTransactionRepository extends JpaRepository<ResourceTransaction, Long> {

    List<ResourceTransaction> findByGame_Id(Long gameId);
}
