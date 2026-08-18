package br.com.flow_assistant.domain.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class RoomBookingTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 17);
    private static final int BUFFER_MINUTES = 10;

    private RoomBooking booking(String start, String end) {
        return new RoomBooking(null, null, 1L, DATE, LocalTime.parse(start), LocalTime.parse(end),
                null, null, RequestStatus.CONFIRMED);
    }

    @Nested
    class IsWithinBusinessHours {

        private static final LocalTime BUSINESS_START = LocalTime.of(8, 0);
        private static final LocalTime BUSINESS_END = LocalTime.of(18, 0);

        @Test
        void dentroDoHorario_retornaTrue() {
            assertThat(booking("09:00", "10:00").isWithinBusinessHours(BUSINESS_START, BUSINESS_END)).isTrue();
        }

        @Test
        void exatamenteNosLimites_inclusive_retornaTrue() {
            assertThat(booking("08:00", "18:00").isWithinBusinessHours(BUSINESS_START, BUSINESS_END)).isTrue();
        }

        @Test
        void comecaAntesDoHorarioComercial_retornaFalse() {
            assertThat(booking("07:59", "10:00").isWithinBusinessHours(BUSINESS_START, BUSINESS_END)).isFalse();
        }

        @Test
        void terminaDepoisDoHorarioComercial_retornaFalse() {
            assertThat(booking("17:00", "18:01").isWithinBusinessHours(BUSINESS_START, BUSINESS_END)).isFalse();
        }
    }

    @Nested
    class ConflictsWithBuffer {

        @Test
        void horariosSobrepostos_conflitam() {
            RoomBooking existing = booking("09:00", "10:00");
            RoomBooking candidate = booking("09:30", "10:30");

            assertThat(candidate.conflictsWithBuffer(existing, BUFFER_MINUTES)).isTrue();
        }

        @Test
        void umaContidaDentroDaOutra_conflita() {
            RoomBooking existing = booking("10:00", "10:30");
            RoomBooking candidate = booking("09:00", "11:00");

            assertThat(candidate.conflictsWithBuffer(existing, BUFFER_MINUTES)).isTrue();
        }

        @Test
        void semGapNenhum_backToBack_conflitaPorCausaDoBuffer() {
            RoomBooking existing = booking("09:00", "10:00");
            RoomBooking candidate = booking("10:00", "10:30");

            assertThat(candidate.conflictsWithBuffer(existing, BUFFER_MINUTES)).isTrue();
        }

        @Test
        void gapMenorQueOBuffer_conflita() {
            RoomBooking existing = booking("09:00", "10:00");
            RoomBooking candidate = booking("10:09", "10:39"); // só 9 min de intervalo, buffer exige 10

            assertThat(candidate.conflictsWithBuffer(existing, BUFFER_MINUTES)).isTrue();
        }

        @Test
        void gapExatamenteIgualAoBuffer_naoConflita() {
            RoomBooking existing = booking("09:00", "10:00");
            RoomBooking candidate = booking("10:10", "10:40"); // exatamente 10 min de intervalo

            assertThat(candidate.conflictsWithBuffer(existing, BUFFER_MINUTES)).isFalse();
        }

        @Test
        void gapExatamenteIgualAoBuffer_antesDaExistente_naoConflita() {
            RoomBooking existing = booking("10:00", "10:30");
            RoomBooking candidate = booking("09:00", "09:50"); // termina 10 min antes da existente começar

            assertThat(candidate.conflictsWithBuffer(existing, BUFFER_MINUTES)).isFalse();
        }

        @Test
        void bemDistante_naoConflita() {
            RoomBooking existing = booking("09:00", "10:00");
            RoomBooking candidate = booking("14:00", "15:00");

            assertThat(candidate.conflictsWithBuffer(existing, BUFFER_MINUTES)).isFalse();
        }

        @Test
        void bufferZero_permiteBackToBackExato() {
            RoomBooking existing = booking("09:00", "10:00");
            RoomBooking candidate = booking("10:00", "10:30");

            assertThat(candidate.conflictsWithBuffer(existing, 0)).isFalse();
        }
    }
}
