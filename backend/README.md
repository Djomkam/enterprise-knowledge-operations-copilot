## Backend - Enterprise Knowledge Operations Copilot

Spring Boot modular monolith built with Java 21.

### Tech Stack

- Java 21
- Gradle 8.6
- Spring Boot 3.2.4
- Spring Security with JWT
- PostgreSQL + pgvector
- RabbitMQ for async processing
- MinIO for object storage
- Spring AI 1.0.0-M4 for LLM integration
- LangChain4j 0.27.1 for agent orchestration
- Flyway for database migrations

### Module Structure

```
/auth       - JWT authentication and authorization
/users      - User management and profiles
/teams      - Team-based access control
/documents  - Document metadata and storage
/ingestion  - Document processing and chunking
/retrieval  - Vector search and semantic retrieval
/chat       - Conversational interface
/memory     - Context and conversation persistence
/audit      - Event logging and compliance
/admin      - System operations and monitoring
/ai         - LLM and embedding abstraction layer
/shared     - Shared utilities and base classes
```

### Running Locally

1. **Start dependencies** (PostgreSQL, RabbitMQ, MinIO):
   ```bash
   cd ../infra
   docker-compose up -d postgres rabbitmq minio
   ```

2. **Set environment variables**:
   ```bash
   export OPENAI_API_KEY=your-key-here
   # Or configure for Ollama
   export AI_PROVIDER=ollama
   ```

3. **Run application**:
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=local'
   ```

4. **Access Swagger UI**:
   http://localhost:8080/swagger-ui.html

### Building

```bash
./gradlew build
```

### Testing

```bash
./gradlew test
```

### Database Migrations

Migrations run automatically on startup via Flyway.

Migration files: `src/main/resources/db/migration/`

To create a new migration:
1. Create file: `V{version}__{description}.sql`
2. Restart application

### AI Integration Architecture

**Spring AI** is used for:
- LLM client abstraction (OpenAI, Ollama)
- Embedding generation
- Vector store integration (pgvector)

**LangChain4j** is used for:
- Multi-agent orchestration
- Agent planning and reasoning
- Tool use and function calling

This separation keeps LLM provider concerns (Spring AI) separate from orchestration logic (LangChain4j).

### Configuration

See `application.yml` for default configuration.

Override with environment variables or profiles:
- `application-local.yml` - Local development
- `application-dev.yml` - Dev environment

### Security

- All endpoints except `/api/v1/auth/**` require JWT authentication
- Role-based access control: ADMIN, USER, ANALYST
- Team-scoped document access
- Audit logging for sensitive operations

### Next Implementation Steps

1. Implement `EmbeddingService` with Spring AI
2. Implement `ChatModelClient` with Spring AI
3. Implement `AIOrchestrationService` with LangChain4j
4. Implement document ingestion pipeline
5. Implement vector search with pgvector
6. Implement multi-agent chat flow
7. Implement memory systems
8. Add comprehensive tests

See `/docs/architecture/` for detailed design.
