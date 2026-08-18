package br.com.flow_assistant.infrastructure.persistence.adapter;

import br.com.flow_assistant.application.port.BookingRepositoryPort;
import br.com.flow_assistant.domain.model.RequestStatus;
import br.com.flow_assistant.domain.model.RoomBooking;
import br.com.flow_assistant.infrastructure.persistence.entity.RequestEntity;
import br.com.flow_assistant.infrastructure.persistence.entity.RoomBookingRequestEntity;
import br.com.flow_assistant.infrastructure.persistence.repository.RequestJpaRepository;
import br.com.flow_assistant.infrastructure.persistence.repository.RoomBookingRequestJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Adaptador de persistência de reservas. Esconde do domínio o detalhe de que
 * uma reserva confirmada é gravada em duas tabelas: primeiro uma linha em
 * {@code requests} (o "envelope" da solicitação) e depois a reserva de sala em
 * {@code room_booking_requests}.
 */
@Component
public class BookingRepositoryAdapter implements BookingRepositoryPort {

    // MVP roda com usuário fixo/mockado, sem login real (document.md, seção 2).
    private static final Long MOCKED_USER_ID = 1L;

    private final RequestJpaRepository requestJpa;
    private final RoomBookingRequestJpaRepository bookingJpa;

    public BookingRepositoryAdapter(RequestJpaRepository requestJpa, RoomBookingRequestJpaRepository bookingJpa) {
        this.requestJpa = requestJpa;
        this.bookingJpa = bookingJpa;
    }

    @Override
    public List<RoomBooking> findByRoomAndDate(Long roomId, LocalDate date) {
        return bookingJpa.findAllByRoomIdAndBookingDate(roomId, date).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public RoomBooking save(RoomBooking booking) {
        RequestEntity request = new RequestEntity();
        request.setType("ROOM_BOOKING");
        request.setStatus(booking.status().name());
        request.setCreatedBy(MOCKED_USER_ID);
        request.setCreatedAt(LocalDateTime.now());
        requestJpa.save(request);

        RoomBookingRequestEntity entity = new RoomBookingRequestEntity();
        entity.setRequestId(request.getId());
        entity.setRoomId(booking.roomId());
        entity.setBookingDate(booking.bookingDate());
        entity.setStartTime(booking.startTime());
        entity.setEndTime(booking.endTime());
        entity.setAttendeesCount(booking.attendeesCount());
        entity.setPurpose(booking.purpose());
        bookingJpa.save(entity);

        return toDomain(entity, booking.status());
    }

    private RoomBooking toDomain(RoomBookingRequestEntity entity) {
        return toDomain(entity, RequestStatus.CONFIRMED);
    }

    private RoomBooking toDomain(RoomBookingRequestEntity entity, RequestStatus status) {
        return new RoomBooking(entity.getId(), entity.getRequestId(), entity.getRoomId(),
                entity.getBookingDate(), entity.getStartTime(), entity.getEndTime(),
                entity.getAttendeesCount(), entity.getPurpose(), status);
    }
}
