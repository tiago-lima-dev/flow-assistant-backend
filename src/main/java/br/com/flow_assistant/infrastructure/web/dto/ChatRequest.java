package br.com.flow_assistant.infrastructure.web.dto;

import java.util.List;

public record ChatRequest(List<ChatTurn> messages) {
}
