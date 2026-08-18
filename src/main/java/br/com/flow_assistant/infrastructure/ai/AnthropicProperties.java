package br.com.flow_assistant.infrastructure.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "anthropic")
public record AnthropicProperties(String apiKey, String model, String apiBaseUrl, String apiVersion) {
}
