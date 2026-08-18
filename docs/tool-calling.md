# O loop de tool calling, por que cada decisão foi tomada

O [README](../README.md) mostra o formato geral do loop agente em
`AssistantChatService`. Esse doc explica o **porquê** de cada decisão de
design ali dentro, a maioria delas nasceu de um bug real encontrado testando
o assistente como se fosse um usuário de verdade, não de teoria.

## O código, como está hoje

```java
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
```

## Por que `while` e não `if`

A primeira versão disso era um `if`: manda a mensagem, se vier `tool_use`
executa a ferramenta, manda o resultado de volta, pega a resposta final.
Parecia suficiente, até um cenário de teste expor o problema:

> **Cenário 6**, usuário pede pra reservar uma sala. O modelo respondeu
> "Vou criar a reserva..." e a conversa terminou aí. Conferindo o banco:
> **zero linhas novas**. O log mostrava só **1 tool round**, e a ferramenta
> chamada nesse round tinha sido `check_room_availability`, não
> `create_booking`.

O que acontecia: pra completar um pedido, o Claude às vezes precisa de
**mais de uma chamada de ferramenta em sequência**, `list_rooms`, depois
`check_room_availability`, depois só então `create_booking`, e nem sempre
ele agrupa duas chamadas no mesmo turno. Um `if` executa uma rodada de
ferramentas e para; se a resposta seguinte *também* vier com `tool_use` (em
vez do texto final), esse `if` já não chama mais nada, a conversa acaba com
o modelo tendo "decidido" chamar `create_booking` sem que o código chegasse
a rodá-la. Daí a mensagem de sucesso ser pura alucinação: o modelo gera o
texto seguinte assumindo que a ferramenta rodou, mas o loop nunca chegou lá.

Trocar por `while` resolve isso: cada iteração processa uma resposta com
`tool_use`, manda o resultado de volta, e só sai do loop quando a resposta
não pede mais ferramentas (ou o limite de segurança bate). Testado de novo
depois da troca: a reserva passou a ser criada de verdade no banco.

## Por que existe um `MAX_TOOL_ROUNDS`

Um `while (response.hasToolUse())` sem limite é um loop que só termina se o
modelo "decidir" parar de pedir ferramentas. Isso é um contrato implícito
com um sistema externo (a API da Anthropic), nada garante que ele sempre
vai convergir pra uma resposta em texto. `MAX_TOOL_ROUNDS = 5` é só uma
trava de segurança: na prática, os fluxos desse assistente (listar → checar
disponibilidade → criar) nunca passam de 2–3 rounds, então 5 dá folga sem
deixar a requisição do usuário pendurada indefinidamente se algo se
comportar de forma inesperada. Se o limite for atingido, `chat()` devolve
uma mensagem de erro amigável em vez de travar:

```java
String text = extractText(response);
if (text.isBlank() && response.hasToolUse()) {
    return "Desculpe, não consegui concluir essa operação agora. Pode tentar de novo?";
}
```

## Por que o histórico vive no cliente, não no servidor

`AssistantChatService.chat(List<ChatTurn> conversation)` recebe a conversa
inteira a cada chamada, o Flutter reenvia todas as mensagens anteriores
junto com a nova, e o backend não guarda nada entre requisições.

Isso foi uma escolha deliberada pra não pagar o custo de uma tabela
`conversations`/`messages` com `conversation_id`, sessão, paginação etc.
antes de precisar dela de verdade. A API da Anthropic já é stateless por
natureza, ela só enxerga o que está no array `messages` de cada request,
então bastava replicar isso um nível acima, no cliente, pra o assistente
"lembrar" o contexto dentro de uma conversa.

Trade-offs conscientes dessa escolha:

- **Sem continuidade entre dispositivos/sessões.** Se o usuário trocar de
  aparelho ou o app for reinstalado, a conversa começa do zero, o
  histórico só existe na memória do `ChatState` do Flutter.
- **Custo de tokens cresce com a conversa.** Cada mensagem nova reenvia
  tudo que veio antes; conversas muito longas ficam caras e lentas. Não é
  um problema hoje (esse é um assistente de tarefa curta, reservar uma
  sala, não um chat de uso contínuo), mas é o primeiro limite real que
  bateria se o escopo mudasse.
- **Sem histórico persistido pra auditoria/analytics.** Não dá pra revisar
  depois "o que os usuários pediram" sem instrumentar isso separadamente.

Persistência server-side (`conversation_id`) seguiu no roadmap do
[README](../README.md#roadmap) como Fase 3, pra quando algum desses
trade-offs virar um problema real em vez de hipotético.

## Por que existe uma "REGRA CRÍTICA" no system prompt

O loop `while` resolveu o caso em que o modelo *parava* de chamar
ferramentas cedo demais. Mas surgiu uma variante mais sutil no mesmo teste
de cenários: o modelo, em alguns casos, respondia **"Reserva confirmada!"**
com todos os detalhes formatados bonitinho, sem o log mostrar nenhum tool
round naquele turno. Ou seja, tecnicamente nada no código estava quebrado
(o loop processava tool use corretamente quando ele existia), o modelo
estava simplesmente optando por responder em texto direto, sem pedir a
ferramenta, e "preenchendo" o resultado da reserva por conta própria.

Isso não tem correção no nível de loop/código: o `while` só reage a
`tool_use` quando ele existe na resposta; não existe um jeito de forçar
"você é obrigado a chamar essa ferramenta agora" via API, quem decide
chamar ou não é o modelo. A única alavanca é instruir isso explicitamente
no prompt:

```text
REGRA CRÍTICA: uma reserva só existe de verdade depois que você chamar a
ferramenta create_booking e receber o resultado dela. Nunca diga que uma
reserva foi "criada", "confirmada" ou está "pronta" sem ter chamado
create_booking NESTE MESMO turno e recebido o resultado. Assim que o usuário
confirmar os dados apresentados (ex: "sim", "pode confirmar", "confirmado"),
sua PRÓXIMA ação é chamar create_booking, não é escrever uma mensagem de
sucesso diretamente.
```

Validado repetindo o mesmo cenário de confirmação **5 vezes seguidas**: 0/1
correto antes da instrução, 5/5 correto depois. Não é uma garantia
matemática (é um LLM, não uma trava de código), mas reduziu a taxa de
alucinação observada de "acontece com frequência" pra "não reproduzido em 5
tentativas".

A regra de negócio continua sendo aplicada no backend de qualquer forma,
`CreateBookingUseCase` valida capacidade, horário comercial e conflito de
novo, independente do que o modelo "acha", mas sem essa instrução, o
usuário recebia uma mensagem de sucesso mesmo quando **nada** tinha sido
persistido, o que é pior que um erro claro.

## Tratamento de erro: por que dois `@ExceptionHandler` pra falha de rede

```java
@ExceptionHandler(RestClientResponseException.class)
public ResponseEntity<Map<String, String>> handleLlmProviderError(RestClientResponseException ex) { ... }

@ExceptionHandler(RestClientException.class)
public ResponseEntity<Map<String, String>> handleLlmConnectionError(RestClientException ex) { ... }
```

`RestClientResponseException` só é lançada quando a Anthropic **respondeu**
com um status de erro (401, 429, 500...), nesse caso dá pra inspecionar o
código e devolver uma mensagem específica (ex: 429 vira "limite de uso ou
créditos esgotados"). Mas falha de rede, timeout, conexão recusada,
conexão derrubada no meio, não produz uma resposta HTTP nenhuma, então
`RestClientResponseException` nunca é lançada nesses casos; o Spring lança
`RestClientException` (a superclasse) direto. Sem o segundo handler, esses
erros de I/O apareciam como 500 cru pro cliente, sem mensagem tratada.

Isso apareceu na prática como um `IOException: Operation timed out`, uma
conexão do pool do `RestClient` ficou "zumbi" (ociosa por tempo demais,
mas ainda no pool de conexões reaproveitáveis) e a chamada seguinte travou
nela até estourar o timeout do sistema operacional, bem mais longo do que
qualquer timeout configurado explicitamente.

## Por que o `AnthropicClient` configura timeout explícito

```java
HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();
JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
requestFactory.setReadTimeout(Duration.ofSeconds(45));
```

Sem `readTimeout` explícito, uma conexão parada (zumbi, rede caída, API
travada) fica pendurada até o timeout default do SO, minutos, não
segundos, deixando a requisição do usuário (e a thread do servidor) presa
esse tempo todo. 45 segundos foi calibrado pra dar folga suficiente pra uma
chamada com tool use (que envolve o modelo "pensar" + potencialmente vários
rounds) sem deixar o usuário esperando por tempo indefinido quando algo
realmente travou. `connectTimeout` de 10s cobre o caso mais simples (não
conseguir nem abrir a conexão).

## As ferramentas expostas nunca pulam a validação de negócio

`ToolExecutor` traduz a chamada da IA pra uma chamada de use case, o
modelo nunca fala com o banco diretamente, nem decide sozinho se uma
reserva é válida. Isso significa que mesmo se o modelo "errar" (por exemplo
tentar reservar fora do horário comercial), o pior caso é o
`BusinessRuleException` voltar como resultado da ferramenta, e o modelo
tem que lidar com esse erro na conversa, a regra em si nunca é
contornável só porque a IA "decidiu" outra coisa. A camada de IA é *só*
uma interface de linguagem natural em cima dos mesmos use cases da Fase 1
([`CreateBookingUseCase`](../src/main/java/br/com/flow_assistant/application/usecase/CreateBookingUseCase.java),
[`CheckRoomAvailabilityUseCase`](../src/main/java/br/com/flow_assistant/application/usecase/CheckRoomAvailabilityUseCase.java)).

## Consciência de tempo no prompt

O system prompt inclui a data **e hora atuais** (`LocalDateTime.now()`), não
só a data:

```java
Hoje é %s, agora são %s (horário local da empresa).
```

Isso não era assim originalmente, só tinha a data. O problema apareceu
testando o cenário "preciso de uma sala agora mesmo, rápido": o modelo, sem
saber que horas eram, tinha que perguntar "qual horário?", o que contradiz
o próprio pedido do usuário (que já disse "agora"). Passar a hora atual no
prompt, combinado com uma heurística explícita ("agora" = a partir do
horário informado acima; sem duração = assuma 30 min; sem sala = escolha
uma com capacidade suficiente sozinho), permitiu ao modelo resolver o pedido
com o mínimo de perguntas, mas ainda **sempre confirmando os dados antes de
chamar `create_booking`**, nunca pulando a confirmação, só as perguntas
redundantes.
