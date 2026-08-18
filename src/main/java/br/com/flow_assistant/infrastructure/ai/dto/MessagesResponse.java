package br.com.flow_assistant.infrastructure.ai.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record MessagesResponse(List<ContentBlock> content, String stopReason) {

    public boolean hasToolUse() {
        return content != null && content.stream().anyMatch(ContentBlock::isToolUseBlock);
    }
}
