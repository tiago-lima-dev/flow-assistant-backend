package br.com.flow_assistant.infrastructure.web.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record CreateBookingRequest(
        Long roomId,
        LocalDate bookingDate,
        LocalTime startTime,
        LocalTime endTime,
        Integer attendeesCount,
        String purpose
) {
}
