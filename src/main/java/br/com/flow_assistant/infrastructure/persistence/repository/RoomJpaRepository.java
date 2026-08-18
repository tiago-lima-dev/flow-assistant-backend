package br.com.flow_assistant.infrastructure.persistence.repository;

import br.com.flow_assistant.infrastructure.persistence.entity.RoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomJpaRepository extends JpaRepository<RoomEntity, Long> {

    List<RoomEntity> findAllByActiveTrue();

    Optional<RoomEntity> findByNameIgnoreCase(String name);
}
