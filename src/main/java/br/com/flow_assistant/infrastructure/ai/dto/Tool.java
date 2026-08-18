package br.com.flow_assistant.infrastructure.ai.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record Tool(String name, String description, Map<String, Object> inputSchema) {
}
