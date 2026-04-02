# Implementation Roadmap

Priority-ordered tasks to complete the Enterprise Knowledge Operations Copilot.

## Phase 1: Core Infrastructure (Weeks 1-2)

### 1. Complete Spring AI Integration
**Priority: P0**
- Implement `EmbeddingService` using Spring AI `EmbeddingClient`
- Configure OpenAI embedding client (or Ollama for local dev)
- Test embedding generation with sample documents
- Add retry logic and rate limiting

**Files to implement:**
- `backend/src/main/java/io/innovation/ekoc/ai/service/impl/EmbeddingServiceImpl.java`
- Configuration in `AIConfig.java`

**Acceptance Criteria:**
- Generate embeddings for single text
- Batch embed 100+ texts efficiently
- Handle API errors gracefully

---

### 2. Implement Document Ingestion Pipeline
**Priority: P0**
- Create document processors (PDF, Text, Markdown)
- Implement chunking service (configurable size/overlap)
- Implement `IngestionService` with RabbitMQ integration
- Create ingestion event handler
- Store chunks with embeddings in PostgreSQL

**Files to implement:**
- `backend/src/main/java/io/innovation/ekoc/ingestion/processor/*.java`
- `backend/src/main/java/io/innovation/ekoc/ingestion/service/ChunkingService.java`
- `backend/src/main/java/io/innovation/ekoc/ingestion/service/IngestionService.java`
- `backend/src/main/java/io/innovation/ekoc/ingestion/service/IngestionEventHandler.java`

**Acceptance Criteria:**
- Upload PDF → chunks stored in DB with embeddings
- RabbitMQ async processing working
- Handle failures with retry logic
- Progress tracking visible

---

### 3. Implement Vector Search (Retrieval)
**Priority: P0**
- Configure Spring AI `PgVectorStore`
- Implement `VectorSearchService` using pgvector
- Implement `RetrievalService` with filters
- Tune IVFFlat index parameters
- Add team-based access control to queries

**Files to implement:**
- `backend/src/main/java/io/innovation/ekoc/retrieval/service/VectorSearchService.java`
- `backend/src/main/java/io/innovation/ekoc/retrieval/service/RetrievalService.java`

**Acceptance Criteria:**
- Similarity search returns top-k chunks
- Results filtered by team membership
- Query performance <100ms for 10k vectors
- Relevance scores accurate

---

## Phase 2: AI Agents & Chat (Weeks 3-4)

### 4. Implement Chat Model Client
**Priority: P0**
- Implement `ChatModelClient` using Spring AI `ChatClient`
- Support OpenAI and Ollama providers
- Add prompt template support
- Implement token counting

**Files to implement:**
- `backend/src/main/java/io/innovation/ekoc/ai/service/impl/ChatModelClientImpl.java`
- `backend/src/main/java/io/innovation/ekoc/ai/provider/OpenAIProvider.java`
- `backend/src/main/java/io/innovation/ekoc/ai/provider/OllamaProvider.java`

**Acceptance Criteria:**
- Complete chat requests successfully
- Swap between OpenAI/Ollama via config
- Handle streaming responses (future)
- Error handling and retries

---

### 5. Implement Multi-Agent Orchestration
**Priority: P0**
- Implement `PlannerAgent` with LangChain4j
- Implement `RetrieverAgent` (delegates to VectorSearchService)
- Implement `PolicyAgent` for access control
- Implement `AnswerComposer` for RAG
- Wire up `AIOrchestrationService`

**Files to implement:**
- `backend/src/main/java/io/innovation/ekoc/ai/agent/impl/*.java`
- `backend/src/main/java/io/innovation/ekoc/ai/service/impl/AIOrchestrationServiceImpl.java`

**Acceptance Criteria:**
- End-to-end chat flow: query → plan → retrieve → filter → answer
- Citations included in response
- Grounded answers (no hallucination when context empty)
- Agent reasoning visible in logs

---

### 6. Implement Chat Service & Controller
**Priority: P1**
- Implement `ChatService`
- Implement `ConversationService`
- Complete `ChatController`
- Add conversation persistence
- Add message history retrieval

**Files to implement:**
- `backend/src/main/java/io/innovation/ekoc/chat/service/ChatService.java`
- `backend/src/main/java/io/innovation/ekoc/chat/service/ConversationService.java`
- Complete `ChatController.java`

**Acceptance Criteria:**
- Create conversations
- Send messages and get AI responses
- Retrieve conversation history
- Citations displayed correctly

---

## Phase 3: Document Management (Weeks 5-6)

### 7. Implement Document Upload & Management
**Priority: P1**
- Complete `DocumentService`
- Implement `DocumentStorageService` (MinIO)
- Complete `DocumentController`
- Add document listing, search, delete
- Add team assignment

**Files to implement:**
- `backend/src/main/java/io/innovation/ekoc/documents/service/DocumentService.java`
- `backend/src/main/java/io/innovation/ekoc/documents/service/DocumentStorageService.java`
- Complete `DocumentController.java`

**Acceptance Criteria:**
- Upload documents via API
- Store in MinIO successfully
- Trigger ingestion pipeline
- List documents with filters
- Delete documents (cascade chunks)

---

### 8. Implement Team Management
**Priority: P1**
- Implement `TeamService`
- Complete `TeamController`
- Add user-team membership management
- Enforce team-scoped document access

**Files to implement:**
- `backend/src/main/java/io/innovation/ekoc/teams/service/TeamService.java`
- Complete `TeamController.java`

**Acceptance Criteria:**
- Create teams
- Add/remove team members
- Assign documents to teams
- Enforce access control

---

## Phase 4: Memory & Advanced Features (Weeks 7-8)

### 9. Implement Memory System
**Priority: P2**
- Implement `MemoryService`
- Implement `MemoryIndexService`
- Add short-term memory (conversation context)
- Add long-term memory (user facts)
- Integrate memory into chat flow

**Files to implement:**
- `backend/src/main/java/io/innovation/ekoc/memory/service/MemoryService.java`
- `backend/src/main/java/io/innovation/ekoc/memory/service/MemoryIndexService.java`

**Acceptance Criteria:**
- Store user preferences
- Retrieve relevant memories during chat
- Semantic search over memories
- Memory expiration/cleanup

---

### 10. Implement Audit Logging
**Priority: P2**
- Implement `AuditService`
- Create `AuditAspect` for automatic logging
- Add audit endpoints for admins
- Log all sensitive operations

**Files to implement:**
- `backend/src/main/java/io/innovation/ekoc/audit/service/AuditService.java`
- `backend/src/main/java/io/innovation/ekoc/audit/aspect/AuditAspect.java`

**Acceptance Criteria:**
- Login/logout logged
- Document operations logged
- Chat queries logged
- Admin can view audit trail

---

## Phase 5: Frontend & Polish (Weeks 9-10)

### 11. Complete Frontend Document Management
**Priority: P1**
- Document upload component
- Document list with search/filters
- Document detail view
- Delete confirmation
- Team assignment UI

---

### 12. Complete Frontend Chat Interface
**Priority: P1**
- Chat message input
- Message history display
- Citation display component
- Conversation list
- Create new conversation
- Real-time typing indicators (future)

---

### 13. Complete Admin Dashboard
**Priority: P2**
- System health metrics
- User management UI
- Team management UI
- Audit log viewer
- Analytics dashboard

---

## Ongoing Tasks

### Testing
- Unit tests for services (target: 80% coverage)
- Integration tests with Testcontainers
- E2E tests with Playwright
- Load testing with JMeter

### Documentation
- API documentation (OpenAPI/Swagger)
- User guide
- Admin guide
- Developer onboarding
- Runbook for operations

### DevOps
- CI/CD pipeline (GitHub Actions)
- Automated testing
- Docker image optimization
- Kubernetes manifests (when ready)
- Monitoring and alerting setup

---

## Future Enhancements

### Advanced Features
- Streaming chat responses
- Multi-modal support (images)
- Advanced analytics
- Document versioning
- Collaborative annotations
- Export chat transcripts
- API rate limiting
- Webhook integrations

### Scalability
- Extract ingestion service
- Read replicas for search
- Cache layer (Redis)
- CDN for static assets
- Message queue clustering

### AI Capabilities
- Fine-tuned embeddings
- Custom models
- Multi-language support
- Domain-specific agents
- Automated summarization
- Question generation
