package br.com.flow_assistant.infrastructure.ai;

import br.com.flow_assistant.application.port.RoomRepositoryPort;
import br.com.flow_assistant.application.usecase.CheckRoomAvailabilityUseCase;
import br.com.flow_assistant.application.usecase.CreateBookingUseCase;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ToolExecutorTest {

    @Mock
    private RoomRepositoryPort roomRepository;
    @Mock
    private CheckRoomAvailabilityUseCase checkRoomAvailabilityUseCase;
    @Mock
    private CreateBookingUseCase createBookingUseCase;

    private ObjectMapper objectMapper;
    private ToolExecutor toolExecutor;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder().build();
        toolExecutor = new ToolExecutor(objectMapper, roomRepository, checkRoomAvailabilityUseCase, createBookingUseCase);
    }

    private Room room(Long id, String name, int capacity) {
        return new Room(id, name, capacity, "2º andar", "TV, Videoconferência", true);
    }

    private RoomBooking booking(Long id, Long roomId, String date, String start, String end, Integer attendees) {
        return new RoomBooking(id, 99L, roomId, LocalDate.parse(date), LocalTime.parse(start),
                LocalTime.parse(end), attendees, null, RequestStatus.CONFIRMED);
    }

    private ObjectNode args() {
        return objectMapper.createObjectNode();
    }

    private JsonNode parse(String json) {
        return objectMapper.readTree(json);
    }

    @Test
    void listRooms_devolveTodasAsSalasAtivas() {
        when(roomRepository.findAllActive()).thenReturn(List.of(
                room(1L, "Sala Vitória", 4),
                room(2L, "Sala Everest", 8)
        ));

        String result = toolExecutor.execute("list_rooms", args());

        JsonNode json = parse(result);
        assertThat(json.get("rooms")).hasSize(2);
        assertThat(json.get("rooms").get(0).get("name").asText()).isEqualTo("Sala Vitória");
        assertThat(json.get("rooms").get(0).get("capacity").asInt()).isEqualTo(4);
        assertThat(json.get("rooms").get(1).get("name").asText()).isEqualTo("Sala Everest");
    }

    @Test
    void checkRoomAvailability_salaDisponivel() {
        when(roomRepository.findByName("Sala Vitória")).thenReturn(Optional.of(room(1L, "Sala Vitória", 4)));
        when(checkRoomAvailabilityUseCase.execute(eq(1L), eq(LocalDate.of(2026, 8, 17)),
                eq(LocalTime.of(9, 0)), eq(LocalTime.of(10, 0)))).thenReturn(true);

        ObjectNode input = args();
        input.put("room_name", "Sala Vitória");
        input.put("date", "2026-08-17");
        input.put("start_time", "09:00");
        input.put("end_time", "10:00");

        String result = toolExecutor.execute("check_room_availability", input);

        JsonNode json = parse(result);
        assertThat(json.get("room_name").asText()).isEqualTo("Sala Vitória");
        assertThat(json.get("available").asBoolean()).isTrue();
    }

    @Test
    void checkRoomAvailability_salaInexistente_devolveErroSemLancarExcecao() {
        when(roomRepository.findByName("Sala Fantasma")).thenReturn(Optional.empty());

        ObjectNode input = args();
        input.put("room_name", "Sala Fantasma");
        input.put("date", "2026-08-17");
        input.put("start_time", "09:00");
        input.put("end_time", "10:00");

        String result = toolExecutor.execute("check_room_availability", input);

        JsonNode json = parse(result);
        assertThat(json.get("error").asText()).contains("Sala Fantasma").contains("list_rooms");
    }

    @Test
    void createBooking_sucesso_devolveDadosDaReserva() {
        when(roomRepository.findByName("Sala Aconcágua")).thenReturn(Optional.of(room(3L, "Sala Aconcágua", 12)));
        when(createBookingUseCase.execute(eq(3L), eq(LocalDate.of(2026, 8, 17)),
                eq(LocalTime.of(17, 0)), eq(LocalTime.of(17, 30)), eq(8), eq("Reunião de time")))
                .thenReturn(booking(16L, 3L, "2026-08-17", "17:00", "17:30", 8));

        ObjectNode input = args();
        input.put("room_name", "Sala Aconcágua");
        input.put("date", "2026-08-17");
        input.put("start_time", "17:00");
        input.put("end_time", "17:30");
        input.put("attendees_count", 8);
        input.put("purpose", "Reunião de time");

        String result = toolExecutor.execute("create_booking", input);

        JsonNode json = parse(result);
        assertThat(json.get("booking_id").asLong()).isEqualTo(16L);
        assertThat(json.get("room_name").asText()).isEqualTo("Sala Aconcágua");
        assertThat(json.get("date").asText()).isEqualTo("2026-08-17");
        assertThat(json.get("status").asText()).isEqualTo("CONFIRMED");
    }

    @Test
    void createBooking_semCamposOpcionais_passaNullParaOUseCase() {
        when(roomRepository.findByName("Sala Aconcágua")).thenReturn(Optional.of(room(3L, "Sala Aconcágua", 12)));
        when(createBookingUseCase.execute(any(), any(), any(), any(), isNull(), isNull()))
                .thenReturn(booking(17L, 3L, "2026-08-17", "17:00", "17:30", null));

        ObjectNode input = args();
        input.put("room_name", "Sala Aconcágua");
        input.put("date", "2026-08-17");
        input.put("start_time", "17:00");
        input.put("end_time", "17:30");

        toolExecutor.execute("create_booking", input);

        ArgumentCaptor<Integer> attendeesCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> purposeCaptor = ArgumentCaptor.forClass(String.class);
        verify(createBookingUseCase).execute(any(), any(), any(), any(), attendeesCaptor.capture(), purposeCaptor.capture());
        assertThat(attendeesCaptor.getValue()).isNull();
        assertThat(purposeCaptor.getValue()).isNull();
    }

    @Test
    void createBooking_regraDeNegocioViolada_devolveErroSemLancarExcecao() {
        when(roomRepository.findByName("Sala Vitória")).thenReturn(Optional.of(room(1L, "Sala Vitória", 4)));
        when(createBookingUseCase.execute(any(), any(), any(), any(), any(), any()))
                .thenThrow(new BusinessRuleException("Número de participantes excede a capacidade da sala."));

        ObjectNode input = args();
        input.put("room_name", "Sala Vitória");
        input.put("date", "2026-08-17");
        input.put("start_time", "09:00");
        input.put("end_time", "10:00");
        input.put("attendees_count", 20);

        String result = toolExecutor.execute("create_booking", input);

        JsonNode json = parse(result);
        assertThat(json.get("error").asText()).isEqualTo("Número de participantes excede a capacidade da sala.");
    }

    @Test
    void ferramentaDesconhecida_devolveErroDescritivo() {
        String result = toolExecutor.execute("cancel_booking", args());

        JsonNode json = parse(result);
        assertThat(json.get("error").asText()).isEqualTo("Ferramenta desconhecida: cancel_booking");
    }

    @Test
    void argumentosFaltandoCausandoErroInesperado_naoPropagaExcecao() {
        // sala existe, mas "date" está ausente -> args.get("date") é null -> NPE ao
        // chamar .asText(), capturado pelo catch genérico (não pelo de BusinessRuleException)
        when(roomRepository.findByName("Sala Vitória")).thenReturn(Optional.of(room(1L, "Sala Vitória", 4)));

        ObjectNode input = args();
        input.put("room_name", "Sala Vitória");

        String result = toolExecutor.execute("check_room_availability", input);

        JsonNode json = parse(result);
        assertThat(json.get("error").asText()).startsWith("Erro ao executar a ferramenta:");
    }
}
