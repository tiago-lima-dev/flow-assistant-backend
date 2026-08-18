package br.com.flow_assistant.infrastructure.ai;

import br.com.flow_assistant.application.port.RoomRepositoryPort;
import br.com.flow_assistant.application.usecase.CheckRoomAvailabilityUseCase;
import br.com.flow_assistant.application.usecase.CreateBookingUseCase;
import br.com.flow_assistant.domain.exception.BusinessRuleException;
import br.com.flow_assistant.domain.model.Room;
import br.com.flow_assistant.domain.model.RoomBooking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
public class ToolExecutor {

    private static final Logger log = LoggerFactory.getLogger(ToolExecutor.class);

    private final ObjectMapper objectMapper;
    private final RoomRepositoryPort roomRepository;
    private final CheckRoomAvailabilityUseCase checkRoomAvailabilityUseCase;
    private final CreateBookingUseCase createBookingUseCase;

    public ToolExecutor(ObjectMapper objectMapper, RoomRepositoryPort roomRepository,
                         CheckRoomAvailabilityUseCase checkRoomAvailabilityUseCase,
                         CreateBookingUseCase createBookingUseCase) {
        this.objectMapper = objectMapper;
        this.roomRepository = roomRepository;
        this.checkRoomAvailabilityUseCase = checkRoomAvailabilityUseCase;
        this.createBookingUseCase = createBookingUseCase;
    }

    public String execute(String toolName, JsonNode args) {
        log.info("Tool call -> {} args={}", toolName, args);
        try {
            String result = switch (toolName) {
                case "list_rooms" -> listRooms();
                case "check_room_availability" -> checkRoomAvailability(args);
                case "create_booking" -> createBooking(args);
                default -> errorJson("Ferramenta desconhecida: " + toolName);
            };
            log.info("Tool result <- {} result={}", toolName, result);
            return result;
        } catch (BusinessRuleException e) {
            log.info("Tool result <- {} businessRuleError={}", toolName, e.getMessage());
            return errorJson(e.getMessage());
        } catch (Exception e) {
            return errorJson("Erro ao executar a ferramenta: " + e.getMessage());
        }
    }

    private String listRooms() {
        List<Room> rooms = roomRepository.findAllActive();
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode array = result.putArray("rooms");
        for (Room room : rooms) {
            ObjectNode node = array.addObject();
            node.put("name", room.name());
            node.put("capacity", room.capacity());
            node.put("location", room.location());
            node.put("equipment", room.equipment());
        }
        return result.toString();
    }

    private String checkRoomAvailability(JsonNode args) {
        Room room = requireRoom(args.get("room_name").asText());
        boolean available = checkRoomAvailabilityUseCase.execute(
                room.id(),
                LocalDate.parse(args.get("date").asText()),
                LocalTime.parse(args.get("start_time").asText()),
                LocalTime.parse(args.get("end_time").asText())
        );
        ObjectNode result = objectMapper.createObjectNode();
        result.put("room_name", room.name());
        result.put("available", available);
        return result.toString();
    }

    private String createBooking(JsonNode args) {
        Room room = requireRoom(args.get("room_name").asText());
        Integer attendeesCount = args.hasNonNull("attendees_count") ? args.get("attendees_count").asInt() : null;
        String purpose = args.hasNonNull("purpose") ? args.get("purpose").asText() : null;

        RoomBooking booking = createBookingUseCase.execute(
                room.id(),
                LocalDate.parse(args.get("date").asText()),
                LocalTime.parse(args.get("start_time").asText()),
                LocalTime.parse(args.get("end_time").asText()),
                attendeesCount,
                purpose
        );

        ObjectNode result = objectMapper.createObjectNode();
        result.put("booking_id", booking.id());
        result.put("room_name", room.name());
        result.put("date", booking.bookingDate().toString());
        result.put("start_time", booking.startTime().toString());
        result.put("end_time", booking.endTime().toString());
        result.put("status", booking.status().name());
        return result.toString();
    }

    private Room requireRoom(String roomName) {
        return roomRepository.findByName(roomName)
                .orElseThrow(() -> new BusinessRuleException(
                        "Sala '" + roomName + "' não encontrada. Use a ferramenta list_rooms para ver as salas disponíveis."));
    }

    private String errorJson(String message) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("error", message);
        return node.toString();
    }
}
