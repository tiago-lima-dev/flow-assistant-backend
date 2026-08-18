package br.com.flow_assistant.application.usecase;

import br.com.flow_assistant.application.port.BookingRepositoryPort;
import br.com.flow_assistant.application.port.RoomRepositoryPort;
import br.com.flow_assistant.domain.exception.BusinessRuleException;
import br.com.flow_assistant.domain.model.RequestStatus;
import br.com.flow_assistant.domain.model.RoomBooking;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

@Component
public class CheckRoomAvailabilityUseCase {

    private static final int BUFFER_MINUTES = 10;

    private final RoomRepositoryPort roomRepository;
    private final BookingRepositoryPort bookingRepository;

    public CheckRoomAvailabilityUseCase(RoomRepositoryPort roomRepository, BookingRepositoryPort bookingRepository) {
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
    }

    public boolean execute(Long roomId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (!roomRepository.existsById(roomId)) {
            throw new BusinessRuleException("Sala não encontrada.");
        }

        RoomBooking candidate = new RoomBooking(null, null, roomId, date, startTime, endTime, null, null, RequestStatus.CONFIRMED);

        return bookingRepository.findByRoomAndDate(roomId, date).stream()
                .noneMatch(existing -> candidate.conflictsWithBuffer(existing, BUFFER_MINUTES));
    }
}
