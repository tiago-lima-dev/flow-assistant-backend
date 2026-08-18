package br.com.flow_assistant.infrastructure.web;

import br.com.flow_assistant.application.port.RoomRepositoryPort;
import br.com.flow_assistant.domain.model.Room;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoomController.class)
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomRepositoryPort roomRepository;

    @Test
    void getRooms_devolveAsSalasAtivasMapeadasParaOResponse() throws Exception {
        when(roomRepository.findAllActive()).thenReturn(List.of(
                new Room(1L, "Sala Vitória", 4, "1º andar", "TV", true),
                new Room(2L, "Sala Everest", 8, "2º andar", "Projetor, Videoconferência", true)
        ));

        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Sala Vitória"))
                .andExpect(jsonPath("$[0].capacity").value(4))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].name").value("Sala Everest"));
    }

    @Test
    void getRooms_semSalasAtivas_devolveListaVazia() throws Exception {
        when(roomRepository.findAllActive()).thenReturn(List.of());

        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
