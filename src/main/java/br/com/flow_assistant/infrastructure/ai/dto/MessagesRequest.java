package br.com.flow_assistant.infrastructure.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MessagesRequest(String model, int maxTokens, String system, List<Message> messages, List<Tool> tools) {
}
