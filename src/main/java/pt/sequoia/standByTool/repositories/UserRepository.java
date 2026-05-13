package pt.sequoia.standByTool.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pt.sequoia.standByTool.models.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' AND u.isEmployee = true")
    List<User> findAllActiveEmployees();

    // Adicione este método para o login funcionar
    Optional<User> findByGoogleOauthId(String googleOauthId);

}