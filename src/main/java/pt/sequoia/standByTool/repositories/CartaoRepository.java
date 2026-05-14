package pt.sequoia.standByTool.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.sequoia.standByTool.models.Cartao;

import java.util.List;

public interface CartaoRepository extends JpaRepository<Cartao, Long> {

    // Devolve apenas os cartões que estão prontos a usar (não perdidos/caducados)
    List<Cartao> findByAtivoTrue();

    // Ajuda-nos a validar se alguém está a tentar criar um cartão com um código que já existe
    boolean existsByIdentificacaoCartao(String identificacaoCartao);
}