package br.com.flow_assistant.infrastructure.web;

import br.com.flow_assistant.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void businessRuleException_devolve400ComAMensagemOriginal() {
        ResponseEntity<Map<String, String>> response = handler.handleBusinessRule(
                new BusinessRuleException("Sala já reservada nesse horário."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Sala já reservada nesse horário.");
    }

    @Test
    void anthropicRespondeu429_devolve502ComMensagemDeLimiteDeUso() {
        RestClientResponseException ex = new RestClientResponseException(
                "429 Too Many Requests", 429, "Too Many Requests", null, null, null);

        ResponseEntity<Map<String, String>> response = handler.handleLlmProviderError(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).containsEntry("message",
                "O provedor de IA recusou a chamada (limite de uso ou créditos esgotados).");
    }

    @Test
    void anthropicRespondeuOutroErro_devolve502ComMensagemGenerica() {
        RestClientResponseException ex = new RestClientResponseException(
                "500 Internal Server Error", 500, "Internal Server Error", null, null, null);

        ResponseEntity<Map<String, String>> response = handler.handleLlmProviderError(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).containsEntry("message", "Não foi possível falar com o provedor de IA no momento.");
    }

    @Test
    void anthropicRespondeu401_devolve502ComMensagemGenerica_naoVazaDetalheDeAuth() {
        RestClientResponseException ex = new RestClientResponseException(
                "401 Unauthorized", 401, "Unauthorized", null, null, null);

        ResponseEntity<Map<String, String>> response = handler.handleLlmProviderError(ex);

        assertThat(response.getBody()).containsEntry("message", "Não foi possível falar com o provedor de IA no momento.");
    }

    @Test
    void falhaDeIO_semRespostaHttp_devolve502ComMensagemDeTenteDeNovo() {
        // Simula o caso real documentado: timeout de conexão, sem resposta HTTP nenhuma
        // da Anthropic, por isso RestClientResponseException não é lançada, e sim a
        // superclasse RestClientException (aqui, ResourceAccessException).
        ResourceAccessException ex = new ResourceAccessException("I/O error", new SocketTimeoutException("timeout"));

        ResponseEntity<Map<String, String>> response = handler.handleLlmConnectionError(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).containsEntry("message",
                "Não foi possível falar com o provedor de IA no momento. Tente de novo.");
    }
}
