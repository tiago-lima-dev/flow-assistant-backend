package br.com.flow_assistant.infrastructure.persistence.adapter;

import br.com.flow_assistant.domain.model.Room;
import br.com.flow_assistant.infrastructure.persistence.entity.RoomEntity;
import br.com.flow_assistant.infrastructure.persistence.repository.RoomJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomRepositoryAdapterTest {

    @Mock
    private RoomJpaRepository jpa;

    private RoomRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RoomRepositoryAdapter(jpa);
    }

    private RoomEntity entity(Long id, String name, int capacity, boolean active) {
        RoomEntity e = new RoomEntity();
        ReflectionTestUtils.setField(e, "id", id);
        e.setName(name);
        e.setCapacity(capacity);
        e.setLocation("2º andar");
        e.setEquipment("TV");
        e.setActive(active);
        return e;
    }

    @Test
    void findAllActive_mapeiaEntitiesParaDominio() {
        when(jpa.findAllByActiveTrue()).thenReturn(List.of(entity(1L, "Sala Vitória", 4, true)));

        List<Room> rooms = adapter.findAllActive();

        assertThat(rooms).hasSize(1);
        Room room = rooms.get(0);
        assertThat(room.id()).isEqualTo(1L);
        assertThat(room.name()).isEqualTo("Sala Vitória");
        assertThat(room.capacity()).isEqualTo(4);
        assertThat(room.active()).isTrue();
    }

    @Test
    void findByName_delegaParaIgnoreCaseEMapeia() {
        when(jpa.findByNameIgnoreCase("sala everest")).thenReturn(Optional.of(entity(2L, "Sala Everest", 8, true)));

        Optional<Room> room = adapter.findByName("sala everest");

        assertThat(room).isPresent();
        assertThat(room.get().name()).isEqualTo("Sala Everest");
    }

    @Test
    void findByName_inexistente_devolveEmpty() {
        when(jpa.findByNameIgnoreCase("Fantasma")).thenReturn(Optional.empty());

        assertThat(adapter.findByName("Fantasma")).isEmpty();
    }

    @Test
    void existsById_delegaParaOJpa() {
        when(jpa.existsById(5L)).thenReturn(true);

        assertThat(adapter.existsById(5L)).isTrue();
    }
}
