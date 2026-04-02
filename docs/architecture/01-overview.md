# Architecture Overview

## System Architecture

Enterprise Knowledge Operations Copilot (EKOC) is a production-grade internal AI platform designed as a **modular monolith** with clear module boundaries, enabling future migration to microservices when needed.

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                       Frontend (React)                       │
│                  TypeScript + React Router                   │
└────────────────────────────┬────────────────────────────────┘
                             │ REST API
┌────────────────────────────┴────────────────────────────────┐
│                    Backend (Spring Boot)                     │
│                     Modular Monolith                         │
├──────────────────────────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │   Auth   │  │  Users   │  │  Teams   │  │Documents │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │Ingestion │  │Retrieval │  │   Chat   │  │  Memory  │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
│  ┌──────────┐  ┌──────────┐                                │
│  │  Audit   │  │  Admin   │                                │
│  └──────────┘  └──────────┘                                │
├──────────────────────────────────────────────────────────────┤
│                    AI Abstraction Layer                      │
│  ┌────────────────────────────────────────────────────────┐ │
│  │  Spring AI (LLM Clients) + LangChain4j (Orchestration)│ │
│  └────────────────────────────────────────────────────────┘ │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────┴────────────────────────────────┐
│                     Data & Messaging                         │
│  ┌─────────────┐  ┌──────────┐  ┌────────┐  ┌──────────┐  │
│  │ PostgreSQL  │  │ pgvector │  │ MinIO  │  │ RabbitMQ │  │
│  │  (Primary)  │  │ (Vectors)│  │(Storage)│ │ (Queue)  │  │
│  └─────────────┘  └──────────┘  └────────┘  └──────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## Design Principles

1. **Modular Monolith First**: Start with clear module boundaries, migrate to microservices when scale demands it
2. **Clear Separation of Concerns**: Each module has well-defined responsibilities
3. **Domain-Driven Design**: Modules aligned with business domains
4. **API-First**: REST APIs designed for external consumption and future decoupling
5. **Database per Module Pattern**: Logical separation with potential for future physical separation

## Technology Stack

### Backend
- **Language**: Java 21
- **Framework**: Spring Boot 3.2.4
- **Security**: Spring Security + JWT
- **ORM**: Spring Data JPA (Hibernate)
- **Migrations**: Flyway
- **AI Integration**:
  - Spring AI (LLM client abstraction)
  - LangChain4j (Agent orchestration)
- **Async Processing**: RabbitMQ
- **Storage**: MinIO (S3-compatible)

### Database
- **Primary**: PostgreSQL 16
- **Vector Search**: pgvector extension
- **Indexes**: B-tree, GIN, IVFFlat (for vectors)

### Frontend
- **Framework**: React 18 + TypeScript
- **Routing**: React Router v6
- **State**: React Query (TanStack Query)
- **Build**: Vite

### Infrastructure
- **Containers**: Docker + Docker Compose
- **Future**: Kubernetes (design accommodates this)

## Module Boundaries

Each module is self-contained with:
- `domain/` - JPA entities
- `repository/` - Data access layer
- `service/` - Business logic
- `controller/` - REST endpoints
- `dto/` - Data transfer objects

Modules communicate via:
- Direct service calls (within monolith)
- Events (via RabbitMQ for async operations)
- Shared kernel (in `/shared` module)

## AI Integration Strategy

### Spring AI Responsibilities
- LLM client abstraction (OpenAI, Ollama, etc.)
- Embedding generation
- Vector store integration (pgvector)
- Prompt templating

### LangChain4j Responsibilities
- Multi-agent orchestration
- Agent planning and reasoning
- Tool/function calling
- Agent state management

This separation ensures:
- Provider flexibility (swap LLM providers easily)
- Clear orchestration logic
- Testability (mock LLM calls independently from orchestration)

## Scalability Path

### Current: Monolith
- Single deployment unit
- Shared database
- Vertical scaling

### Future: Microservices
Module boundaries enable extraction:
1. Document Ingestion Service (high CPU/IO)
2. Retrieval Service (vector search optimization)
3. Chat Service (variable load)
4. Core Services (users, teams, auth)

## Security Model

- **Authentication**: JWT tokens (stateless)
- **Authorization**: Role-based (ADMIN, USER, ANALYST)
- **Data Access**: Team-scoped (documents belong to teams)
- **Audit**: All sensitive operations logged
- **Secrets**: Environment variables (no hardcoding)

## Next Steps

See:
- [Module Responsibilities](./02-module-responsibilities.md)
- [Security Model](./03-security-model.md)
- [Memory Model](./04-memory-model.md)
- [Ingestion Pipeline](./05-ingestion-pipeline.md)
