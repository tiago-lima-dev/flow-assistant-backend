package br.com.flow_assistant.infrastructure.web;

import br.com.flow_assistant.infrastructure.ai.AssistantChatService;
import br.com.flow_assistant.infrastructure.web.dto.ChatTurn;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssistantChatService assistantChatService;

    @Test
    void postChatMessages_devolveARespostaDoAssistente() throws Exception {
        when(assistantChatService.chat(any())).thenReturn("Temos 3 salas disponíveis.");

        mockMvc.perform(post("/api/chat/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "messages": [
                                    { "role": "user", "content": "Quais salas existem?" }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Temos 3 salas disponíveis."));

        verify(assistantChatService).chat(eq(List.of(new ChatTurn("user", "Quais salas existem?"))));
    }

    @Test
    void postChatMessages_repassaOHistoricoCompletoNaOrdem() throws Exception {
        when(assistantChatService.chat(any())).thenReturn("ok");

        mockMvc.perform(post("/api/chat/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "messages": [
                                    { "role": "user", "content": "oi" },
                                    { "role": "assistant", "content": "Olá! Em que posso ajudar?" },
                                    { "role": "user", "content": "quero reservar uma sala" }
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        verify(assistantChatService).chat(eq(List.of(
                new ChatTurn("user", "oi"),
                new ChatTurn("assistant", "Olá! Em que posso ajudar?"),
                new ChatTurn("user", "quero reservar uma sala")
        )));
    }

    @Test
    void anthropicIndisponivel_devolve502ComMensagemAmigavel() throws Exception {
        when(assistantChatService.chat(any()))
                .thenThrow(new ResourceAccessException("timeout", new SocketTimeoutException("timeout")));

        mockMvc.perform(post("/api/chat/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"messages": [{ "role": "user", "content": "oi" }]}
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Não foi possível falar com o provedor de IA no momento. Tente de novo."));
    }
}
