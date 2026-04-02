# Enterprise Knowledge Operations Copilot

Production-grade internal AI platform for secure document ingestion, semantic retrieval (RAG), grounded chat with citations, role-based access control, memory systems, and multi-agent orchestration.

## Architecture

**Modular Monolith** → designed for future Kubernetes migration
**Language**: Java 21
**Framework**: Spring Boot 3.x
**Build**: Gradle
**Database**: PostgreSQL + pgvector
**Message Queue**: RabbitMQ
**Object Storage**: MinIO
**Frontend**: React + TypeScript

## Features

- **Document Ingestion**: PDF, Markdown, Text with chunking and embedding
- **Semantic Retrieval**: Vector search with pgvector
- **Grounded Chat**: RAG-based chat with source citations
- **Multi-Agent Orchestration**: Planner, Retriever, Policy, Answer Composer
- **Memory Systems**: Conversation and user context persistence
- **Role-Based Access Control**: JWT-based authentication with team permissions
- **Audit Logging**: Comprehensive event tracking
- **Admin Operations**: System health, user management, analytics

## Quick Start

### Prerequisites

- Java 21
- Docker & Docker Compose
- Node 20+
- Make (optional)

### Local Development

```bash
# Clone and navigate
cd enterprise-knowledge-operations-copilot

# Start infrastructure (postgres, rabbitmq, minio)
make infra-up

# Start backend
make backend-up

# Start frontend (in another terminal)
make frontend-up

# Access the application
# Frontend: http://localhost:5173
# Backend API: http://localhost:8080
# API Docs: http://localhost:8080/swagger-ui.html
```

### Using Docker Compose

```bash
docker-compose up
```

## Project Structure

```
/backend       - Spring Boot modular monolith
/frontend      - React + TypeScript SPA
/infra         - Docker Compose and infrastructure config
/docs          - Architecture docs and ADRs
/scripts       - Helper scripts for local dev
```

## Module Responsibilities

| Module | Responsibility |
|--------|----------------|
| `auth` | JWT authentication and authorization |
| `users` | User management and profiles |
| `teams` | Team-based access control |
| `documents` | Document metadata and storage |
| `ingestion` | Document processing and chunking |
| `retrieval` | Vector search and semantic retrieval |
| `chat` | Conversational interface |
| `memory` | Context and conversation persistence |
| `audit` | Event logging and compliance |
| `admin` | System operations and monitoring |
| `ai` | LLM and embedding abstraction layer |

## Configuration

See `/infra/.env.template` for required environment variables.

Key configurations:
- Database connection
- JWT secret
- MinIO credentials
- RabbitMQ connection
- LLM provider API keys (OpenAI, Ollama, etc.)

## Development

### Running Tests

```bash
cd backend
./gradlew test
```

### Database Migrations

Migrations run automatically on startup via Flyway. To create new migration:

```bash
# Create new migration file in src/main/resources/db/migration/
# Follow naming: V{version}__{description}.sql
```

### API Documentation

OpenAPI/Swagger available at `/swagger-ui.html` when running locally.

## Deployment

See `/docs/architecture/` for deployment guidelines.

Current setup targets local development. Kubernetes manifests will be added in future iterations.

## Security

- JWT-based authentication
- Role-based access control (ADMIN, USER, ANALYST)
- Team-scoped document access
- Audit logging for all sensitive operations
- Secrets managed via environment variables

## Next Steps

See [docs/architecture/01-overview.md](docs/architecture/01-overview.md) for detailed architecture.

See project board for implementation roadmap.

## License

Internal use only.
