package br.com.flow_assistant.infrastructure.ai;

import br.com.flow_assistant.infrastructure.ai.dto.ContentBlock;
import br.com.flow_assistant.infrastructure.ai.dto.MessagesRequest;
import br.com.flow_assistant.infrastructure.ai.dto.MessagesResponse;
import br.com.flow_assistant.infrastructure.web.dto.ChatTurn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testa diretamente o loop de tool calling descrito em docs/tool-calling.md,
 * em especial o comportamento que motivou trocar o {@code if} original por um
 * {@code while} (Cenário 6: resposta com tool_use que nunca era executada).
 */
@ExtendWith(MockitoExtension.class)
class AssistantChatServiceTest {

    @Mock
    private AnthropicClient anthropicClient;
    @Mock
    private ToolExecutor toolExecutor;

    private AssistantChatService service;

    @BeforeEach
    void setUp() {
        AnthropicProperties properties = new AnthropicProperties("test-key", "claude-haiku-4-5",
                "https://api.anthropic.com/v1", "2023-06-01");
        service = new AssistantChatService(anthropicClient, toolExecutor, properties);
    }

    private static ContentBlock toolUseBlock(String id, String name) {
        ContentBlock block = new ContentBlock();
        block.setType("tool_use");
        block.setId(id);
        block.setName(name);
        block.setInput(JsonMapper.builder().build().createObjectNode());
        return block;
    }

    private static MessagesResponse textResponse(String text) {
        return new MessagesResponse(List.of(ContentBlock.text(text)), "end_turn");
    }

    private static MessagesResponse toolUseResponse(String toolId, String toolName) {
        return new MessagesResponse(List.of(toolUseBlock(toolId, toolName)), "tool_use");
    }

    @Test
    void semToolUse_devolveTextoDiretoSemExecutarFerramentas() {
        when(anthropicClient.createMessage(any())).thenReturn(textResponse("Temos 3 salas disponíveis."));

        String reply = service.chat(List.of(new ChatTurn("user", "Quais salas existem?")));

        assertThat(reply).isEqualTo("Temos 3 salas disponíveis.");
        verify(anthropicClient, times(1)).createMessage(any());
        verifyNoInteractions(toolExecutor);
    }

    @Test
    void umaRodadaDeToolUse_executaFerramentaEDevolveTextoFinal() {
        ObjectNode emptyArgs = JsonMapper.builder().build().createObjectNode();
        when(anthropicClient.createMessage(any()))
                .thenReturn(toolUseResponse("toolu_1", "list_rooms"))
                .thenReturn(textResponse("Não há salas cadastradas."));
        when(toolExecutor.execute(eq("list_rooms"), any())).thenReturn("{\"rooms\":[]}");

        String reply = service.chat(List.of(new ChatTurn("user", "Quais salas existem?")));

        assertThat(reply).isEqualTo("Não há salas cadastradas.");
        verify(anthropicClient, times(2)).createMessage(any());
        verify(toolExecutor, times(1)).execute(eq("list_rooms"), any());
    }

    @Test
    void duasRodadasEncadeadasDeToolUse_processaAmbasAntesDoTextoFinal() {
        // Reproduz o próprio Cenário 6 do doc: checar disponibilidade e SÓ DEPOIS
        // criar a reserva, em dois turnos de tool_use separados, é exatamente o
        // caso que o antigo `if` (uma única rodada) deixava passar batido.
        when(anthropicClient.createMessage(any()))
                .thenReturn(toolUseResponse("toolu_1", "check_room_availability"))
                .thenReturn(toolUseResponse("toolu_2", "create_booking"))
                .thenReturn(textResponse("Reserva confirmada!"));
        when(toolExecutor.execute(eq("check_room_availability"), any())).thenReturn("{\"available\":true}");
        when(toolExecutor.execute(eq("create_booking"), any())).thenReturn("{\"booking_id\":16}");

        String reply = service.chat(List.of(new ChatTurn("user", "Reserva a Aconcágua amanhã às 17h")));

        assertThat(reply).isEqualTo("Reserva confirmada!");
        verify(anthropicClient, times(3)).createMessage(any());

        InOrder order = inOrder(toolExecutor);
        order.verify(toolExecutor).execute(eq("check_room_availability"), any());
        order.verify(toolExecutor).execute(eq("create_booking"), any());
        order.verifyNoMoreInteractions();
    }

    @Test
    void modeloNuncaParaDePedirFerramentas_paraNoLimiteDeRoundsEDevolveFallback() {
        when(anthropicClient.createMessage(any())).thenReturn(toolUseResponse("toolu_x", "list_rooms"));
        when(toolExecutor.execute(any(), any())).thenReturn("{\"rooms\":[]}");

        String reply = service.chat(List.of(new ChatTurn("user", "quero uma sala")));

        assertThat(reply).isEqualTo("Desculpe, não consegui concluir essa operação agora. Pode tentar de novo?");
        // 1 chamada inicial + 5 rounds do MAX_TOOL_ROUNDS = 6 chamadas à API
        verify(anthropicClient, times(6)).createMessage(any());
        // a ferramenta só roda pra cada resposta processada dentro do loop, não pra última (6ª)
        verify(toolExecutor, times(5)).execute(any(), any());
    }

    @Test
    void historicoDaConversaEhTraduzidoParaMensagensNaOrdemCorreta() {
        when(anthropicClient.createMessage(any())).thenReturn(textResponse("Olá! Como posso ajudar?"));

        service.chat(List.of(
                new ChatTurn("user", "oi"),
                new ChatTurn("assistant", "Olá! Em que posso ajudar?"),
                new ChatTurn("user", "quero reservar uma sala")
        ));

        ArgumentCaptor<MessagesRequest> captor = ArgumentCaptor.forClass(MessagesRequest.class);
        verify(anthropicClient).createMessage(captor.capture());

        MessagesRequest request = captor.getValue();
        assertThat(request.messages()).hasSize(3);
        assertThat(request.messages().get(0).role()).isEqualTo("user");
        assertThat(request.messages().get(0).content().get(0).getText()).isEqualTo("oi");
        assertThat(request.messages().get(1).role()).isEqualTo("assistant");
        assertThat(request.messages().get(2).content().get(0).getText()).isEqualTo("quero reservar uma sala");
        assertThat(request.model()).isEqualTo("claude-haiku-4-5");
        assertThat(request.tools()).isEqualTo(ToolCatalog.TOOLS);
        assertThat(request.system()).isNotBlank();
    }

    @Test
    void conversaVazia_naoQuebraEEnviaListaVaziaDeMensagens() {
        when(anthropicClient.createMessage(any())).thenReturn(textResponse("Em que posso ajudar?"));

        String reply = service.chat(List.of());

        assertThat(reply).isEqualTo("Em que posso ajudar?");
        ArgumentCaptor<MessagesRequest> captor = ArgumentCaptor.forClass(MessagesRequest.class);
        verify(anthropicClient).createMessage(captor.capture());
        assertThat(captor.getValue().messages()).isEmpty();
    }
}
