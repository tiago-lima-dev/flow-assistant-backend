package br.com.flow_assistant.infrastructure.web;

import br.com.flow_assistant.application.usecase.CheckRoomAvailabilityUseCase;
import br.com.flow_assistant.application.usecase.CreateBookingUseCase;
import br.com.flow_assistant.domain.exception.BusinessRuleException;
import br.com.flow_assistant.domain.model.RequestStatus;
import br.com.flow_assistant.domain.model.RoomBooking;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateBookingUseCase createBookingUseCase;

    @MockitoBean
    private CheckRoomAvailabilityUseCase checkRoomAvailabilityUseCase;

    @Test
    void postBookings_dadosValidos_devolve201ComAReservaCriada() throws Exception {
        RoomBooking booking = new RoomBooking(16L, 42L, 3L, LocalDate.of(2026, 8, 17),
                LocalTime.of(17, 0), LocalTime.of(17, 30), 8, "Planejamento", RequestStatus.CONFIRMED);

        when(createBookingUseCase.execute(eq(3L), eq(LocalDate.of(2026, 8, 17)),
                eq(LocalTime.of(17, 0)), eq(LocalTime.of(17, 30)), eq(8), eq("Planejamento")))
                .thenReturn(booking);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 3,
                                  "bookingDate": "2026-08-17",
                                  "startTime": "17:00",
                                  "endTime": "17:30",
                                  "attendeesCount": 8,
                                  "purpose": "Planejamento"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(16))
                .andExpect(jsonPath("$.requestId").value(42))
                .andExpect(jsonPath("$.roomId").value(3))
                .andExpect(jsonPath("$.attendeesCount").value(8))
                .andExpect(jsonPath("$.purpose").value("Planejamento"));
    }

    @Test
    void postBookings_regraDeNegocioViolada_devolve400ComAMensagem() throws Exception {
        when(createBookingUseCase.execute(any(), any(), any(), any(), anyInt(), any()))
                .thenThrow(new BusinessRuleException("Número de participantes excede a capacidade da sala."));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1,
                                  "bookingDate": "2026-08-17",
                                  "startTime": "09:00",
                                  "endTime": "10:00",
                                  "attendeesCount": 99
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Número de participantes excede a capacidade da sala."));
    }

    @Test
    void getAvailability_devolveDisponibilidadeDaSala() throws Exception {
        when(checkRoomAvailabilityUseCase.execute(eq(3L), eq(LocalDate.of(2026, 8, 17)),
                eq(LocalTime.of(9, 0)), eq(LocalTime.of(10, 0)))).thenReturn(true);

        mockMvc.perform(get("/api/rooms/3/availability")
                        .param("date", "2026-08-17")
                        .param("startTime", "09:00")
                        .param("endTime", "10:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(3))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void getAvailability_salaIndisponivel_devolveFalse() throws Exception {
        when(checkRoomAvailabilityUseCase.execute(eq(3L), any(), any(), any())).thenReturn(false);

        mockMvc.perform(get("/api/rooms/3/availability")
                        .param("date", "2026-08-17")
                        .param("startTime", "09:00")
                        .param("endTime", "10:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }
}
