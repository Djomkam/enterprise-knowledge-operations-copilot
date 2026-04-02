# ADR-003: Separation of Spring AI and LangChain4j Responsibilities

## Status
Accepted

## Context
We need to integrate LLM capabilities and multi-agent orchestration. Two frameworks are available:
- **Spring AI**: Spring ecosystem, LLM client abstraction, vector stores
- **LangChain4j**: Agent orchestration, tool use, planning

Options:
1. Use only Spring AI (implement orchestration ourselves)
2. Use only LangChain4j (use their LLM clients)
3. Use both with clear separation of concerns

## Decision
We will use **both Spring AI and LangChain4j** with clear responsibility separation:
- **Spring AI**: LLM provider abstraction and infrastructure
- **LangChain4j**: Agent orchestration and reasoning logic

## Rationale

### Why Both Frameworks?

1. **Spring AI Strengths**
   - Native Spring Boot integration
   - Consistent configuration (application.yml)
   - First-class support for multiple providers (OpenAI, Ollama, Azure)
   - Vector store integration (pgvector, Redis, etc.)
   - Spring Cloud Stream compatibility
   - Prompt templating with Spring Expression Language

2. **LangChain4j Strengths**
   - Mature agent orchestration patterns
   - Built-in planning and reasoning
   - Tool/function calling abstractions
   - Agent state management
   - Memory management patterns
   - Chain composition

3. **Complementary Not Overlapping**
   - Spring AI: "How do I call the LLM?"
   - LangChain4j: "How do I orchestrate agents?"

### Responsibility Boundaries

```
┌─────────────────────────────────────────────┐
│           LangChain4j Layer                 │
│  ┌──────────────────────────────────────┐  │
│  │  PlannerAgent, RetrieverAgent,       │  │
│  │  PolicyAgent, AnswerComposer         │  │
│  │  (Orchestration Logic)               │  │
│  └────────────┬─────────────────────────┘  │
└───────────────┼─────────────────────────────┘
                │ Delegates LLM calls
┌───────────────┼─────────────────────────────┐
│               ▼                              │
│           Spring AI Layer                    │
│  ┌──────────────────────────────────────┐  │
│  │  ChatClient, EmbeddingClient         │  │
│  │  VectorStore, PromptTemplate         │  │
│  │  (Provider Abstraction)              │  │
│  └──────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

#### Spring AI Responsibilities
- LLM API client implementation
- Embedding generation
- Vector store operations (pgvector)
- Provider configuration (OpenAI, Ollama)
- Rate limiting and retry logic
- Prompt template rendering

#### LangChain4j Responsibilities
- Agent definition (@AiService)
- Multi-agent orchestration
- Planning and reasoning
- Tool execution
- Agent memory management
- Chain of thought patterns

### Implementation Pattern

```java
// Spring AI - LLM Client
@Service
public class ChatModelClientImpl implements ChatModelClient {
    private final ChatClient springAiChatClient; // Spring AI

    public ChatCompletionResponse complete(ChatCompletionRequest request) {
        // Delegate to Spring AI
        return springAiChatClient.call(request);
    }
}

// LangChain4j - Agent Orchestration
@AiService
interface PlannerAgent {
    @SystemMessage("You are a query planner...")
    RetrievalPlan plan(String query);
}

// Orchestration Service - Coordinates both
@Service
public class AIOrchestrationServiceImpl {
    private final PlannerAgent plannerAgent;        // LangChain4j
    private final RetrieverAgent retrieverAgent;    // LangChain4j
    private final ChatModelClient chatClient;       // Spring AI

    public ChatResponse processChat(ChatRequest request) {
        // 1. Plan with LangChain4j agent
        var plan = plannerAgent.plan(request.getMessage());

        // 2. Retrieve with LangChain4j agent + Spring AI vector store
        var context = retrieverAgent.retrieve(plan.getSearchRequest());

        // 3. Generate response with Spring AI
        return chatClient.complete(buildPrompt(context));
    }
}
```

## Consequences

### Positive
- Best-of-breed approach (use each framework's strengths)
- Spring AI provides provider flexibility
- LangChain4j provides orchestration patterns
- Clear separation simplifies testing
- Can swap LLM providers without changing orchestration
- Can swap orchestration without changing LLM integration

### Negative
- Two frameworks to maintain and update
- Potential version conflicts
- Learning curve for both frameworks
- More dependencies in build

### Mitigations
- Centralize LLM calls through Spring AI interfaces
- Keep LangChain4j isolated to agent/orchestration package
- Document boundaries clearly
- Create adapters between frameworks where needed

## Migration Path

If one framework becomes inadequate:
- **Drop LangChain4j**: Implement orchestration with Spring AI patterns
- **Drop Spring AI**: Use LangChain4j's built-in LLM clients
- **Add new framework**: Adapter pattern isolates changes

## References
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [LangChain4j Documentation](https://docs.langchain4j.dev/)
- [Spring AI vs LangChain4j Comparison](https://spring.io/blog/2024/02/20/spring-ai-vs-langchain4j)
