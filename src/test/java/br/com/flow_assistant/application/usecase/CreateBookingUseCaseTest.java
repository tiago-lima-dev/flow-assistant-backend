package br.com.flow_assistant.application.usecase;

import br.com.flow_assistant.application.port.BookingRepositoryPort;
import br.com.flow_assistant.application.port.RoomRepositoryPort;
import br.com.flow_assistant.domain.exception.BusinessRuleException;
import br.com.flow_assistant.domain.model.RequestStatus;
import br.com.flow_assistant.domain.model.Room;
import br.com.flow_assistant.domain.model.RoomBooking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateBookingUseCaseTest {

    private static final Long ROOM_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 8, 17);
    private static final LocalTime START = LocalTime.of(9, 0);
    private static final LocalTime END = LocalTime.of(10, 0);

    @Mock
    private RoomRepositoryPort roomRepository;
    @Mock
    private BookingRepositoryPort bookingRepository;
    @Mock
    private CheckRoomAvailabilityUseCase checkRoomAvailabilityUseCase;

    private CreateBookingUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateBookingUseCase(roomRepository, bookingRepository, checkRoomAvailabilityUseCase);
    }

    private Room activeRoom(int capacity) {
        return new Room(ROOM_ID, "Sala Aconcágua", capacity, "2º andar", "TV", true);
    }

    // Simula o adaptador: devolve a reserva salva com id/requestId atribuídos.
    private void stubSaveAssigningIds() {
        when(bookingRepository.save(any(RoomBooking.class))).thenAnswer(invocation -> {
            RoomBooking candidate = invocation.getArgument(0);
            return new RoomBooking(16L, 42L, candidate.roomId(), candidate.bookingDate(),
                    candidate.startTime(), candidate.endTime(), candidate.attendeesCount(),
                    candidate.purpose(), candidate.status());
        });
    }

    @Test
    void dadosValidos_criaReservaConfirmadaComIdsAtribuidos() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(activeRoom(12)));
        when(checkRoomAvailabilityUseCase.execute(ROOM_ID, DATE, START, END)).thenReturn(true);
        stubSaveAssigningIds();

        RoomBooking result = useCase.execute(ROOM_ID, DATE, START, END, 8, "Planejamento Q3");

        assertThat(result.id()).isEqualTo(16L);
        assertThat(result.requestId()).isEqualTo(42L);
        assertThat(result.roomId()).isEqualTo(ROOM_ID);
        assertThat(result.bookingDate()).isEqualTo(DATE);
        assertThat(result.startTime()).isEqualTo(START);
        assertThat(result.endTime()).isEqualTo(END);
        assertThat(result.attendeesCount()).isEqualTo(8);
        assertThat(result.purpose()).isEqualTo("Planejamento Q3");
        assertThat(result.status()).isEqualTo(RequestStatus.CONFIRMED);
    }

    @Test
    void salvaCandidataComStatusConfirmado() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(activeRoom(12)));
        when(checkRoomAvailabilityUseCase.execute(ROOM_ID, DATE, START, END)).thenReturn(true);
        stubSaveAssigningIds();

        useCase.execute(ROOM_ID, DATE, START, END, 8, "Planejamento Q3");

        ArgumentCaptor<RoomBooking> captor = ArgumentCaptor.forClass(RoomBooking.class);
        verify(bookingRepository).save(captor.capture());
        RoomBooking saved = captor.getValue();
        assertThat(saved.status()).isEqualTo(RequestStatus.CONFIRMED);
        assertThat(saved.id()).isNull();
        assertThat(saved.attendeesCount()).isEqualTo(8);
    }

    @Test
    void semParticipantesInformados_puloAValidacaoDeCapacidade() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(activeRoom(2)));
        when(checkRoomAvailabilityUseCase.execute(ROOM_ID, DATE, START, END)).thenReturn(true);
        stubSaveAssigningIds();

        RoomBooking result = useCase.execute(ROOM_ID, DATE, START, END, null, null);

        assertThat(result.attendeesCount()).isNull();
    }

    @Test
    void roomIdNulo_lancaExcecaoSemTocarRepositorios() {
        assertThatThrownBy(() -> useCase.execute(null, DATE, START, END, 4, null))
                .isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(roomRepository, bookingRepository, checkRoomAvailabilityUseCase);
    }

    @Test
    void dataNula_lancaExcecao() {
        assertThatThrownBy(() -> useCase.execute(ROOM_ID, null, START, END, 4, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void horarioInicioNulo_lancaExcecao() {
        assertThatThrownBy(() -> useCase.execute(ROOM_ID, DATE, null, END, 4, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void horarioFimNulo_lancaExcecao() {
        assertThatThrownBy(() -> useCase.execute(ROOM_ID, DATE, START, null, 4, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void horarioInicioNaoAntesDoFim_lancaExcecao() {
        assertThatThrownBy(() -> useCase.execute(ROOM_ID, DATE, LocalTime.of(10, 0), LocalTime.of(9, 0), 4, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Horário de início deve ser antes do horário de término.");
    }

    @Test
    void horarioInicioIgualAoFim_lancaExcecao() {
        assertThatThrownBy(() -> useCase.execute(ROOM_ID, DATE, START, START, 4, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void salaNaoEncontrada_lancaExcecao() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(ROOM_ID, DATE, START, END, 4, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Sala não encontrada.");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void salaInativa_lancaExcecao() {
        Room inactive = new Room(ROOM_ID, "Sala Aconcágua", 10, "2º andar", "TV", false);
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> useCase.execute(ROOM_ID, DATE, START, END, 4, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Sala inativa não pode ser reservada.");
    }

    @Test
    void participantesExcedemCapacidade_lancaExcecao() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(activeRoom(4)));

        assertThatThrownBy(() -> useCase.execute(ROOM_ID, DATE, START, END, 5, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Número de participantes excede a capacidade da sala.");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void foraDoHorarioComercial_lancaExcecao() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(activeRoom(10)));

        assertThatThrownBy(() -> useCase.execute(ROOM_ID, DATE, LocalTime.of(19, 0), LocalTime.of(20, 0), 4, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Reserva fora do horário comercial (08:00–18:00).");
    }

    @Test
    void salaIndisponivelNoHorario_lancaExcecaoComMencaoAoBuffer() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(activeRoom(10)));
        when(checkRoomAvailabilityUseCase.execute(ROOM_ID, DATE, START, END)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(ROOM_ID, DATE, START, END, 4, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("buffer de 10 min");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void checaDisponibilidadeAntesDeSalvar_ordemDeChamadas() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(activeRoom(10)));
        when(checkRoomAvailabilityUseCase.execute(ROOM_ID, DATE, START, END)).thenReturn(true);
        stubSaveAssigningIds();

        useCase.execute(ROOM_ID, DATE, START, END, 4, null);

        var inOrder = org.mockito.Mockito.inOrder(checkRoomAvailabilityUseCase, bookingRepository);
        inOrder.verify(checkRoomAvailabilityUseCase).execute(eq(ROOM_ID), eq(DATE), eq(START), eq(END));
        inOrder.verify(bookingRepository).save(any());
    }
}
