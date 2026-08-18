package br.com.flow_assistant.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class RoomTest {

    private Room room(int capacity) {
        return new Room(1L, "Sala Teste", capacity, "1º andar", "TV", true);
    }

    @ParameterizedTest(name = "capacidade {0}, {1} participantes -> {2}")
    @CsvSource({
            "8, 8, true",   // exatamente na capacidade
            "8, 7, true",   // abaixo da capacidade
            "8, 9, false",  // acima da capacidade
            "8, 0, true",   // zero participantes
    })
    void hasCapacityFor_comparaComOLimiteDaSala(int capacity, int attendeesCount, boolean expected) {
        assertThat(room(capacity).hasCapacityFor(attendeesCount)).isEqualTo(expected);
    }

    @Test
    void hasCapacityFor_capacidadeUmParticipante_cabeExatamenteUm() {
        assertThat(room(1).hasCapacityFor(1)).isTrue();
        assertThat(room(1).hasCapacityFor(2)).isFalse();
    }
}
