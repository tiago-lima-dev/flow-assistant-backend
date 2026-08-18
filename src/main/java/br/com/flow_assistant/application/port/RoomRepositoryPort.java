package br.com.flow_assistant.application.port;

import br.com.flow_assistant.domain.model.Room;

import java.util.List;
import java.util.Optional;

/**
 * Porta de saída da camada de aplicação para acesso a salas. Fala apenas em
 * termos de domínio ({@link Room}); a implementação (adaptador) vive na
 * infraestrutura, assim os use cases não conhecem JPA nem Spring Data.
 */
public interface RoomRepositoryPort {

    List<Room> findAllActive();

    Optional<Room> findById(Long id);

    Optional<Room> findByName(String name);

    boolean existsById(Long id);
}
