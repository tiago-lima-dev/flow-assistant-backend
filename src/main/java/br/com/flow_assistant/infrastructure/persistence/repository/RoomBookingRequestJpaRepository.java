package br.com.flow_assistant.infrastructure.persistence.repository;

import br.com.flow_assistant.infrastructure.persistence.entity.RoomBookingRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RoomBookingRequestJpaRepository extends JpaRepository<RoomBookingRequestEntity, Long> {

    List<RoomBookingRequestEntity> findAllByRoomIdAndBookingDate(Long roomId, LocalDate bookingDate);
}
