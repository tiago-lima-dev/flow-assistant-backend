package br.com.flow_assistant.infrastructure.web;

import br.com.flow_assistant.infrastructure.ai.AssistantChatService;
import br.com.flow_assistant.infrastructure.web.dto.ChatRequest;
import br.com.flow_assistant.infrastructure.web.dto.ChatResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final AssistantChatService assistantChatService;

    public ChatController(AssistantChatService assistantChatService) {
        this.assistantChatService = assistantChatService;
    }

    @PostMapping("/api/chat/messages")
    public ChatResponse sendMessage(@RequestBody ChatRequest request) {
        String reply = assistantChatService.chat(request.messages());
        return new ChatResponse(reply);
    }
}
