package br.com.flow_assistant.infrastructure.persistence.adapter;

import br.com.flow_assistant.domain.model.RequestStatus;
import br.com.flow_assistant.domain.model.RoomBooking;
import br.com.flow_assistant.infrastructure.persistence.entity.RequestEntity;
import br.com.flow_assistant.infrastructure.persistence.entity.RoomBookingRequestEntity;
import br.com.flow_assistant.infrastructure.persistence.repository.RequestJpaRepository;
import br.com.flow_assistant.infrastructure.persistence.repository.RoomBookingRequestJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingRepositoryAdapterTest {

    private static final Long ROOM_ID = 3L;
    private static final LocalDate DATE = LocalDate.of(2026, 8, 17);

    @Mock
    private RequestJpaRepository requestJpa;
    @Mock
    private RoomBookingRequestJpaRepository bookingJpa;

    private BookingRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new BookingRepositoryAdapter(requestJpa, bookingJpa);
    }

    @Test
    void save_criaRequestEnvelopeEReserva_devolvendoDominioComIds() {
        // request ganha id ao salvar
        when(requestJpa.save(any(RequestEntity.class))).thenAnswer(inv -> {
            RequestEntity req = inv.getArgument(0);
            ReflectionTestUtils.setField(req, "id", 42L);
            return req;
        });
        // booking ganha id ao salvar
        when(bookingJpa.save(any(RoomBookingRequestEntity.class))).thenAnswer(inv -> {
            RoomBookingRequestEntity e = inv.getArgument(0);
            ReflectionTestUtils.setField(e, "id", 16L);
            return e;
        });

        RoomBooking candidate = new RoomBooking(null, null, ROOM_ID, DATE,
                LocalTime.of(17, 0), LocalTime.of(17, 30), 8, "Planejamento", RequestStatus.CONFIRMED);

        RoomBooking saved = adapter.save(candidate);

        // O envelope Request é criado com os campos de persistência corretos
        ArgumentCaptor<RequestEntity> requestCaptor = ArgumentCaptor.forClass(RequestEntity.class);
        org.mockito.Mockito.verify(requestJpa).save(requestCaptor.capture());
        RequestEntity request = requestCaptor.getValue();
        assertThat(request.getType()).isEqualTo("ROOM_BOOKING");
        assertThat(request.getStatus()).isEqualTo("CONFIRMED");
        assertThat(request.getCreatedBy()).isEqualTo(1L);
        assertThat(request.getCreatedAt()).isNotNull();

        // A reserva é gravada com o requestId do envelope
        ArgumentCaptor<RoomBookingRequestEntity> bookingCaptor = ArgumentCaptor.forClass(RoomBookingRequestEntity.class);
        org.mockito.Mockito.verify(bookingJpa).save(bookingCaptor.capture());
        assertThat(bookingCaptor.getValue().getRequestId()).isEqualTo(42L);

        // O domínio retornado sai com os ids atribuídos
        assertThat(saved.id()).isEqualTo(16L);
        assertThat(saved.requestId()).isEqualTo(42L);
        assertThat(saved.roomId()).isEqualTo(ROOM_ID);
        assertThat(saved.attendeesCount()).isEqualTo(8);
        assertThat(saved.status()).isEqualTo(RequestStatus.CONFIRMED);
    }

    @Test
    void findByRoomAndDate_mapeiaEntitiesParaDominio() {
        RoomBookingRequestEntity e = new RoomBookingRequestEntity();
        ReflectionTestUtils.setField(e, "id", 10L);
        e.setRequestId(20L);
        e.setRoomId(ROOM_ID);
        e.setBookingDate(DATE);
        e.setStartTime(LocalTime.of(9, 0));
        e.setEndTime(LocalTime.of(10, 0));
        e.setAttendeesCount(5);
        when(bookingJpa.findAllByRoomIdAndBookingDate(ROOM_ID, DATE)).thenReturn(List.of(e));

        List<RoomBooking> bookings = adapter.findByRoomAndDate(ROOM_ID, DATE);

        assertThat(bookings).hasSize(1);
        RoomBooking b = bookings.get(0);
        assertThat(b.id()).isEqualTo(10L);
        assertThat(b.roomId()).isEqualTo(ROOM_ID);
        assertThat(b.startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(b.status()).isEqualTo(RequestStatus.CONFIRMED);
    }
}
