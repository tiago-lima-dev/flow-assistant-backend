package br.com.flow_assistant.application.usecase;

import br.com.flow_assistant.application.port.BookingRepositoryPort;
import br.com.flow_assistant.application.port.RoomRepositoryPort;
import br.com.flow_assistant.domain.exception.BusinessRuleException;
import br.com.flow_assistant.domain.model.RequestStatus;
import br.com.flow_assistant.domain.model.RoomBooking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckRoomAvailabilityUseCaseTest {

    private static final Long ROOM_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 8, 17);

    @Mock
    private RoomRepositoryPort roomRepository;

    @Mock
    private BookingRepositoryPort bookingRepository;

    private CheckRoomAvailabilityUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CheckRoomAvailabilityUseCase(roomRepository, bookingRepository);
    }

    private RoomBooking existingBooking(String start, String end) {
        return new RoomBooking(10L, 20L, ROOM_ID, DATE, LocalTime.parse(start), LocalTime.parse(end),
                null, null, RequestStatus.CONFIRMED);
    }

    @Test
    void salaNaoExiste_lancaExcecao() {
        when(roomRepository.existsById(ROOM_ID)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(ROOM_ID, DATE, LocalTime.of(9, 0), LocalTime.of(10, 0)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Sala não encontrada.");
    }

    @Test
    void semReservasNoDia_disponivel() {
        when(roomRepository.existsById(ROOM_ID)).thenReturn(true);
        when(bookingRepository.findByRoomAndDate(ROOM_ID, DATE)).thenReturn(List.of());

        boolean available = useCase.execute(ROOM_ID, DATE, LocalTime.of(9, 0), LocalTime.of(10, 0));

        assertThat(available).isTrue();
    }

    @Test
    void reservaExistenteSemConflito_disponivel() {
        when(roomRepository.existsById(ROOM_ID)).thenReturn(true);
        when(bookingRepository.findByRoomAndDate(ROOM_ID, DATE))
                .thenReturn(List.of(existingBooking("09:00", "10:00")));

        boolean available = useCase.execute(ROOM_ID, DATE, LocalTime.of(14, 0), LocalTime.of(15, 0));

        assertThat(available).isTrue();
    }

    @Test
    void reservaExistenteComConflito_indisponivel() {
        when(roomRepository.existsById(ROOM_ID)).thenReturn(true);
        when(bookingRepository.findByRoomAndDate(ROOM_ID, DATE))
                .thenReturn(List.of(existingBooking("09:00", "10:00")));

        boolean available = useCase.execute(ROOM_ID, DATE, LocalTime.of(9, 30), LocalTime.of(10, 30));

        assertThat(available).isFalse();
    }

    @Test
    void reservaExistenteDentroDoBuffer_indisponivel() {
        when(roomRepository.existsById(ROOM_ID)).thenReturn(true);
        when(bookingRepository.findByRoomAndDate(ROOM_ID, DATE))
                .thenReturn(List.of(existingBooking("09:00", "10:00")));

        // só 5 min de intervalo após a reserva existente, dentro do buffer de 10 min
        boolean available = useCase.execute(ROOM_ID, DATE, LocalTime.of(10, 5), LocalTime.of(10, 30));

        assertThat(available).isFalse();
    }

    @Test
    void umaEntreVariasReservasConflita_indisponivel() {
        when(roomRepository.existsById(ROOM_ID)).thenReturn(true);
        when(bookingRepository.findByRoomAndDate(ROOM_ID, DATE)).thenReturn(List.of(
                existingBooking("08:00", "08:30"),
                existingBooking("09:00", "10:00"), // essa conflita com a candidata
                existingBooking("16:00", "17:00")
        ));

        boolean available = useCase.execute(ROOM_ID, DATE, LocalTime.of(9, 30), LocalTime.of(9, 45));

        assertThat(available).isFalse();
    }

    @Test
    void consultaOFiltroCorreto_porSalaEData() {
        when(roomRepository.existsById(ROOM_ID)).thenReturn(true);
        when(bookingRepository.findByRoomAndDate(any(), any())).thenReturn(List.of());

        useCase.execute(ROOM_ID, DATE, LocalTime.of(9, 0), LocalTime.of(10, 0));

        verify(bookingRepository).findByRoomAndDate(eq(ROOM_ID), eq(DATE));
    }
}
