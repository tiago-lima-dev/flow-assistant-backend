package br.com.flow_assistant.infrastructure.persistence.adapter;

import br.com.flow_assistant.application.port.RoomRepositoryPort;
import br.com.flow_assistant.domain.model.Room;
import br.com.flow_assistant.infrastructure.persistence.entity.RoomEntity;
import br.com.flow_assistant.infrastructure.persistence.repository.RoomJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador que implementa {@link RoomRepositoryPort} sobre o Spring Data,
 * traduzindo {@link RoomEntity} (JPA) para o modelo de domínio {@link Room}.
 */
@Component
public class RoomRepositoryAdapter implements RoomRepositoryPort {

    private final RoomJpaRepository jpa;

    public RoomRepositoryAdapter(RoomJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Room> findAllActive() {
        return jpa.findAllByActiveTrue().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Room> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Room> findByName(String name) {
        return jpa.findByNameIgnoreCase(name).map(this::toDomain);
    }

    @Override
    public boolean existsById(Long id) {
        return jpa.existsById(id);
    }

    private Room toDomain(RoomEntity entity) {
        return new Room(entity.getId(), entity.getName(), entity.getCapacity(),
                entity.getLocation(), entity.getEquipment(), entity.isActive());
    }
}
