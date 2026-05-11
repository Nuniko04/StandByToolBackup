package pt.sequoia.standByTool.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.sequoia.standByTool.models.TurnType;
import java.util.UUID;

public interface TurnTypeRepository extends JpaRepository<TurnType, UUID> {

    // Adicionar esta linha mágica do Spring Data JPA
    TurnType findByName(String name);
}