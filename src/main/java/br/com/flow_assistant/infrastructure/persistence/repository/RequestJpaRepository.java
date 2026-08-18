package br.com.flow_assistant.infrastructure.persistence.repository;

import br.com.flow_assistant.infrastructure.persistence.entity.RequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestJpaRepository extends JpaRepository<RequestEntity, Long> {
}
