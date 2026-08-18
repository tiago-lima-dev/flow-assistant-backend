package br.com.flow_assistant.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record RoomBooking(
        Long id,
        Long requestId,
        Long roomId,
        LocalDate bookingDate,
        LocalTime startTime,
        LocalTime endTime,
        Integer attendeesCount,
        String purpose,
        RequestStatus status
) {

    public boolean isWithinBusinessHours(LocalTime businessStart, LocalTime businessEnd) {
        return !startTime.isBefore(businessStart) && !endTime.isAfter(businessEnd);
    }

    /**
     * Conflita se os intervalos se sobrepõem considerando um buffer mínimo entre reuniões na mesma sala.
     */
    public boolean conflictsWithBuffer(RoomBooking other, int bufferMinutes) {
        LocalTime bufferedStart = other.startTime().minusMinutes(bufferMinutes);
        LocalTime bufferedEnd = other.endTime().plusMinutes(bufferMinutes);
        return startTime.isBefore(bufferedEnd) && bufferedStart.isBefore(endTime);
    }
}
