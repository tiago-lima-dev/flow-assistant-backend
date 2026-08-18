package br.com.flow_assistant.infrastructure.ai;

import br.com.flow_assistant.infrastructure.ai.dto.Tool;

import java.util.List;
import java.util.Map;

/**
 * Contratos das tools alinhados ao document.md (seção 6), a IA só decide qual chamar
 * e com quais parâmetros, toda regra de negócio é validada no backend (use cases da Fase 1).
 */
public final class ToolCatalog {

    private ToolCatalog() {
    }

    public static final List<Tool> TOOLS = List.of(
            new Tool(
                    "list_rooms",
                    "Lista as salas de reunião ativas, com nome, capacidade, localização e equipamentos.",
                    Map.of("type", "object", "properties", Map.of())
            ),
            new Tool(
                    "check_room_availability",
                    "Verifica se uma sala está disponível numa data e intervalo de horário.",
                    Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "room_name", Map.of("type", "string", "description", "Nome exato da sala"),
                                    "date", Map.of("type", "string", "description", "Data no formato YYYY-MM-DD"),
                                    "start_time", Map.of("type", "string", "description", "Hora de início no formato HH:mm"),
                                    "end_time", Map.of("type", "string", "description", "Hora de término no formato HH:mm")
                            ),
                            "required", List.of("room_name", "date", "start_time", "end_time")
                    )
            ),
            new Tool(
                    "create_booking",
                    "Cria uma reserva confirmada de sala de reunião. Só chame depois que o usuário confirmar todos os dados.",
                    Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "room_name", Map.of("type", "string", "description", "Nome exato da sala"),
                                    "date", Map.of("type", "string", "description", "Data no formato YYYY-MM-DD"),
                                    "start_time", Map.of("type", "string", "description", "Hora de início no formato HH:mm"),
                                    "end_time", Map.of("type", "string", "description", "Hora de término no formato HH:mm"),
                                    "attendees_count", Map.of("type", "integer", "description", "Número de participantes"),
                                    "purpose", Map.of("type", "string", "description", "Finalidade da reunião")
                            ),
                            "required", List.of("room_name", "date", "start_time", "end_time")
                    )
            )
    );
}
