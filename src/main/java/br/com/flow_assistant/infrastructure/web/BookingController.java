package br.com.flow_assistant.infrastructure.web;

import br.com.flow_assistant.application.usecase.CheckRoomAvailabilityUseCase;
import br.com.flow_assistant.application.usecase.CreateBookingUseCase;
import br.com.flow_assistant.domain.model.RoomBooking;
import br.com.flow_assistant.infrastructure.web.dto.BookingResponse;
import br.com.flow_assistant.infrastructure.web.dto.CreateBookingRequest;
import br.com.flow_assistant.infrastructure.web.dto.RoomAvailabilityResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;

@RestController
public class BookingController {

    private final CreateBookingUseCase createBookingUseCase;
    private final CheckRoomAvailabilityUseCase checkRoomAvailabilityUseCase;

    public BookingController(CreateBookingUseCase createBookingUseCase,
                              CheckRoomAvailabilityUseCase checkRoomAvailabilityUseCase) {
        this.createBookingUseCase = createBookingUseCase;
        this.checkRoomAvailabilityUseCase = checkRoomAvailabilityUseCase;
    }

    @PostMapping("/api/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@RequestBody CreateBookingRequest request) {
        RoomBooking booking = createBookingUseCase.execute(
                request.roomId(), request.bookingDate(), request.startTime(), request.endTime(),
                request.attendeesCount(), request.purpose());
        return toResponse(booking);
    }

    @GetMapping("/api/rooms/{roomId}/availability")
    public RoomAvailabilityResponse checkAvailability(@PathVariable Long roomId,
                                                        @RequestParam LocalDate date,
                                                        @RequestParam LocalTime startTime,
                                                        @RequestParam LocalTime endTime) {
        boolean available = checkRoomAvailabilityUseCase.execute(roomId, date, startTime, endTime);
        return new RoomAvailabilityResponse(roomId, available);
    }

    private BookingResponse toResponse(RoomBooking booking) {
        return new BookingResponse(booking.id(), booking.requestId(), booking.roomId(),
                booking.bookingDate(), booking.startTime(), booking.endTime(),
                booking.attendeesCount(), booking.purpose());
    }
}
