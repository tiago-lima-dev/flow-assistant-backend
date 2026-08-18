package br.com.flow_assistant.infrastructure.ai;

import br.com.flow_assistant.infrastructure.ai.dto.ContentBlock;
import br.com.flow_assistant.infrastructure.ai.dto.Message;
import br.com.flow_assistant.infrastructure.ai.dto.MessagesRequest;
import br.com.flow_assistant.infrastructure.ai.dto.MessagesResponse;
import br.com.flow_assistant.infrastructure.web.dto.ChatTurn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class AssistantChatService {

    private static final Logger log = LoggerFactory.getLogger(AssistantChatService.class);

    private static final int MAX_TOKENS = 1024;

    // Limite de segurança pra evitar loop indefinido caso o modelo fique
    // pedindo tools sem nunca fechar com uma resposta em texto.
    private static final int MAX_TOOL_ROUNDS = 5;

    private final AnthropicClient anthropicClient;
    private final ToolExecutor toolExecutor;
    private final AnthropicProperties properties;

    public AssistantChatService(AnthropicClient anthropicClient, ToolExecutor toolExecutor, AnthropicProperties properties) {
        this.anthropicClient = anthropicClient;
        this.toolExecutor = toolExecutor;
        this.properties = properties;
    }

    public String chat(List<ChatTurn> conversation) {
        log.info("Chat request -> {} turns, last: [{}] {}",
                conversation.size(),
                conversation.isEmpty() ? "-" : conversation.get(conversation.size() - 1).role(),
                conversation.isEmpty() ? "-" : conversation.get(conversation.size() - 1).content());

        List<Message> messages = new ArrayList<>();
        for (ChatTurn turn : conversation) {
            messages.add(new Message(turn.role(), List.of(ContentBlock.text(turn.content()))));
        }

        MessagesResponse response = anthropicClient.createMessage(
                new MessagesRequest(properties.model(), MAX_TOKENS, systemPrompt(), messages, ToolCatalog.TOOLS));

        int rounds = 0;
        while (response.hasToolUse() && rounds < MAX_TOOL_ROUNDS) {
            messages.add(Message.assistant(response.content()));

            List<ContentBlock> toolResults = new ArrayList<>();
            for (ContentBlock block : response.content()) {
                if (block.isToolUseBlock()) {
                    String result = toolExecutor.execute(block.getName(), block.getInput());
                    toolResults.add(ContentBlock.toolResult(block.getId(), result));
                }
            }
            messages.add(Message.user(toolResults));

            response = anthropicClient.createMessage(
                    new MessagesRequest(properties.model(), MAX_TOKENS, systemPrompt(), messages, ToolCatalog.TOOLS));
            rounds++;
        }
        log.info("Chat response after {} tool round(s), hasToolUse={}", rounds, response.hasToolUse());

        String text = extractText(response);
        if (text.isBlank() && response.hasToolUse()) {
            return "Desculpe, não consegui concluir essa operação agora. Pode tentar de novo?";
        }
        return text;
    }

    private String extractText(MessagesResponse response) {
        StringBuilder text = new StringBuilder();
        for (ContentBlock block : response.content()) {
            if (block.isTextBlock()) {
                text.append(block.getText());
            }
        }
        return text.toString();
    }

    private String systemPrompt() {
        LocalDateTime now = LocalDateTime.now();
        return """
                Você é o assistente de reserva de salas de reunião da empresa.
                Hoje é %s, agora são %s (horário local da empresa).
                Seu papel é ajudar o usuário a reservar, consultar disponibilidade e ver as salas existentes.
                Use as ferramentas disponíveis para consultar dados reais, nunca invente disponibilidade, salas ou reservas.

                Campos obrigatórios pra reservar: sala, data, horário de início e horário de
                término. Participantes e finalidade são OPCIONAIS, só pergunte por eles se o
                usuário já dá abertura pra isso; nunca trave o fluxo por causa deles.

                Se o usuário pedir algo "rápido", "simples" ou "agora mesmo", não faça um
                questionário, use bom senso e resolva com o mínimo de perguntas possível:
                - "agora" ou "agora mesmo" = a partir do horário atual informado acima.
                - Sem duração especificada = assuma 30 minutos.
                - Sem sala especificada = escolha você mesmo uma sala com capacidade
                  suficiente pra quantidade de pessoas mencionada (ou a menor disponível se
                  não foi dito quantas pessoas), usando list_rooms e check_room_availability;
                  não pergunte "qual sala" nesse cenário, só informe qual você escolheu.
                Ainda assim, sempre confirme com o usuário os dados escolhidos antes de
                chamar create_booking, só pule as perguntas redundantes, nunca a confirmação.

                REGRA CRÍTICA: uma reserva só existe de verdade depois que você chamar a
                ferramenta create_booking e receber o resultado dela. Nunca diga que uma
                reserva foi "criada", "confirmada" ou está "pronta" sem ter chamado
                create_booking NESTE MESMO turno e recebido o resultado. Assim que o usuário
                confirmar os dados apresentados (ex: "sim", "pode confirmar", "confirmado"),
                sua PRÓXIMA ação é chamar create_booking, não é escrever uma mensagem de
                sucesso diretamente. Se por algum motivo você não puder chamar a ferramenta,
                diga claramente que não conseguiu concluir, nunca finja que deu certo.

                Você não tem ferramenta para cancelar ou alterar reservas existentes, se
                pedirem isso, diga claramente que essa função ainda não existe, não invente
                um cancelamento.

                Você só trata de reserva de salas de reunião. Se perguntarem qualquer coisa
                fora desse assunto (conhecimentos gerais, código do sistema, opinião, outros
                temas), NÃO responda o assunto, nem para dar uma resposta rápida. Diga de
                forma educada e breve que você só ajuda com reserva de salas e ofereça o que
                você pode fazer (listar salas, verificar disponibilidade, criar reserva).

                Não use emojis nas respostas, o app que exibe essas mensagens não renderiza
                todos os emojis corretamente. Use markdown simples (negrito, listas) pra
                formatação, sem símbolos ou ícones decorativos.

                Responda sempre em português, de forma direta e amigável.
                """.formatted(
                now.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                now.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
    }
}
