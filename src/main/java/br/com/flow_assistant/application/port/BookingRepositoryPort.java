package br.com.flow_assistant.application.port;

import br.com.flow_assistant.domain.model.RoomBooking;

import java.time.LocalDate;
import java.util.List;

/**
 * Porta de saída para persistência de reservas. O use case só conhece o
 * domínio {@link RoomBooking}; o adaptador é quem sabe que uma reserva é
 * gravada em duas tabelas (requests + room_booking_requests).
 */
public interface BookingRepositoryPort {

    List<RoomBooking> findByRoomAndDate(Long roomId, LocalDate date);

    RoomBooking save(RoomBooking booking);
}
