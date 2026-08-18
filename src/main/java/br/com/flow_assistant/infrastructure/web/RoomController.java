package br.com.flow_assistant.infrastructure.web;

import br.com.flow_assistant.application.port.RoomRepositoryPort;
import br.com.flow_assistant.domain.model.Room;
import br.com.flow_assistant.infrastructure.web.dto.RoomResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RoomController {

    private final RoomRepositoryPort roomRepository;

    public RoomController(RoomRepositoryPort roomRepository) {
        this.roomRepository = roomRepository;
    }

    @GetMapping("/api/rooms")
    public List<RoomResponse> list() {
        return roomRepository.findAllActive().stream()
                .map(this::toResponse)
                .toList();
    }

    private RoomResponse toResponse(Room room) {
        return new RoomResponse(room.id(), room.name(), room.capacity(),
                room.location(), room.equipment(), room.active());
    }
}
