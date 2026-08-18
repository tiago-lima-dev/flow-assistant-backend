package br.com.flow_assistant.application.usecase;

import br.com.flow_assistant.application.port.BookingRepositoryPort;
import br.com.flow_assistant.application.port.RoomRepositoryPort;
import br.com.flow_assistant.domain.exception.BusinessRuleException;
import br.com.flow_assistant.domain.model.RequestStatus;
import br.com.flow_assistant.domain.model.Room;
import br.com.flow_assistant.domain.model.RoomBooking;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

@Component
public class CreateBookingUseCase {

    private static final LocalTime BUSINESS_HOURS_START = LocalTime.of(8, 0);
    private static final LocalTime BUSINESS_HOURS_END = LocalTime.of(18, 0);
    private static final int BUFFER_MINUTES = 10;

    private final RoomRepositoryPort roomRepository;
    private final BookingRepositoryPort bookingRepository;
    private final CheckRoomAvailabilityUseCase checkRoomAvailabilityUseCase;

    public CreateBookingUseCase(RoomRepositoryPort roomRepository,
                                 BookingRepositoryPort bookingRepository,
                                 CheckRoomAvailabilityUseCase checkRoomAvailabilityUseCase) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.checkRoomAvailabilityUseCase = checkRoomAvailabilityUseCase;
    }

    public RoomBooking execute(Long roomId, LocalDate bookingDate, LocalTime startTime, LocalTime endTime,
                                Integer attendeesCount, String purpose) {
        if (roomId == null || bookingDate == null || startTime == null || endTime == null) {
            throw new BusinessRuleException("roomId, bookingDate, startTime e endTime são obrigatórios.");
        }
        if (!startTime.isBefore(endTime)) {
            throw new BusinessRuleException("Horário de início deve ser antes do horário de término.");
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessRuleException("Sala não encontrada."));

        if (!room.active()) {
            throw new BusinessRuleException("Sala inativa não pode ser reservada.");
        }
        if (attendeesCount != null && !room.hasCapacityFor(attendeesCount)) {
            throw new BusinessRuleException("Número de participantes excede a capacidade da sala.");
        }

        RoomBooking candidate = new RoomBooking(null, null, roomId, bookingDate, startTime, endTime,
                attendeesCount, purpose, RequestStatus.CONFIRMED);
        if (!candidate.isWithinBusinessHours(BUSINESS_HOURS_START, BUSINESS_HOURS_END)) {
            throw new BusinessRuleException("Reserva fora do horário comercial (08:00–18:00).");
        }
        if (!checkRoomAvailabilityUseCase.execute(roomId, bookingDate, startTime, endTime)) {
            throw new BusinessRuleException("Sala já reservada nesse horário (respeitando o buffer de " + BUFFER_MINUTES + " min entre reuniões).");
        }

        return bookingRepository.save(candidate);
    }
}
