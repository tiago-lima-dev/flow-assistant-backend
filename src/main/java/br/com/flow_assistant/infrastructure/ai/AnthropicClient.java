package br.com.flow_assistant.infrastructure.ai;

import br.com.flow_assistant.infrastructure.ai.dto.MessagesRequest;
import br.com.flow_assistant.infrastructure.ai.dto.MessagesResponse;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class AnthropicClient {

    private final RestClient restClient;

    public AnthropicClient(AnthropicProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        // Chamadas à Anthropic com tool use podem levar alguns segundos; damos folga
        // pra não derrubar a requisição, mas sem deixar uma conexão travada pra sempre
        // (ex: conexão "zumbi" reaproveitada do pool após ficar ociosa).
        requestFactory.setReadTimeout(Duration.ofSeconds(45));

        this.restClient = RestClient.builder()
                .baseUrl(properties.apiBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("x-api-key", properties.apiKey())
                .defaultHeader("anthropic-version", properties.apiVersion())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public MessagesResponse createMessage(MessagesRequest request) {
        return restClient.post()
                .uri("/messages")
                .body(request)
                .retrieve()
                .body(MessagesResponse.class);
    }
}
