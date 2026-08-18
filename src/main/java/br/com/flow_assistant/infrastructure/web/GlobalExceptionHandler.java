package br.com.flow_assistant.infrastructure.web;

import br.com.flow_assistant.domain.exception.BusinessRuleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, String>> handleBusinessRule(BusinessRuleException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<Map<String, String>> handleLlmProviderError(RestClientResponseException ex) {
        log.error("LLM provider error {} - body: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
        String message = ex.getStatusCode().value() == 429
                ? "O provedor de IA recusou a chamada (limite de uso ou créditos esgotados)."
                : "Não foi possível falar com o provedor de IA no momento.";
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", message));
    }

    // Cobre falhas de I/O (timeout, conexão recusada/derrubada) que não têm uma
    // resposta HTTP associada, RestClientResponseException não captura esses casos.
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Map<String, String>> handleLlmConnectionError(RestClientException ex) {
        log.error("LLM connection error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("message", "Não foi possível falar com o provedor de IA no momento. Tente de novo."));
    }
}
