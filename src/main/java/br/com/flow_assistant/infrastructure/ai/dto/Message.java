package br.com.flow_assistant.infrastructure.ai.dto;

import java.util.List;

public record Message(String role, List<ContentBlock> content) {

    public static Message user(String text) {
        return new Message("user", List.of(ContentBlock.text(text)));
    }

    public static Message user(List<ContentBlock> blocks) {
        return new Message("user", blocks);
    }

    public static Message assistant(List<ContentBlock> blocks) {
        return new Message("assistant", blocks);
    }
}
