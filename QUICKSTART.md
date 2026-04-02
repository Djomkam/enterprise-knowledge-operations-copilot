# Quick Start Guide

Get the Enterprise Knowledge Operations Copilot running locally in under 10 minutes.

## Prerequisites

Ensure you have these installed:
- **Java 21** (JDK 21+)
- **Docker** and **Docker Compose**
- **Node.js 20+** and npm
- **Make** (optional, for shortcuts)
- **Git**

Verify installations:
```bash
java --version    # Should show 21.x
docker --version
node --version    # Should show 20.x
```

---

## Step 1: Clone and Setup

```bash
cd enterprise-knowledge-operations-copilot

# Create environment file from template
cp infra/.env.template infra/.env

# Edit .env and set your OpenAI API key (or use Ollama for local)
# Required: OPENAI_API_KEY=your-key-here
# Or:       AI_PROVIDER=ollama
nano infra/.env
```

---

## Step 2: Start Infrastructure

Start PostgreSQL, RabbitMQ, MinIO, and Ollama:

```bash
# Using Make (recommended)
make infra-up

# Or using script
./scripts/start.sh

# Or manually
cd infra
docker compose up -d postgres rabbitmq minio ollama
```

**Wait ~15-20 seconds** for services to be healthy.

Verify services are running:
```bash
docker ps
# Should show: ekoc-postgres, ekoc-rabbitmq, ekoc-minio, ekoc-ollama
```

### Using Ollama (Optional)

If using Ollama for local AI (no OpenAI API key needed):

```bash
# Pull required models (first time only, takes 5-10 minutes)
make ollama-pull

# Verify models are installed
make ollama-models
# Should show: llama2, nomic-embed-text
```

**Note**: Set `AI_PROVIDER=ollama` in your `.env` file (default).

---

## Step 3: Start Backend

In a **new terminal**:

```bash
cd backend

# Run with Gradle
./gradlew bootRun --args='--spring.profiles.active=local'

# Or using Make
make backend-up
```

Backend starts on `http://localhost:8080`

**First run**: Flyway migrations will create all database tables.

**Look for**:
```
Started EnterpriseKnowledgeOperationsCopilotApplication in X seconds
```

---

## Step 4: Start Frontend

In **another terminal**:

```bash
cd frontend

# Install dependencies (first time only)
npm install

# Start dev server
npm run dev

# Or using Make
make frontend-up
```

Frontend starts on `http://localhost:5173`

---

## Step 5: Access the Application

Open your browser:

### Main Application
- **Frontend**: http://localhost:5173
- **Login**: `admin` / `admin123`

### API & Admin Tools
- **Backend API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **MinIO Console**: http://localhost:9091 (`minioadmin` / `minioadmin`)
- **RabbitMQ Admin**: http://localhost:15672 (`guest` / `guest`)

---

## Step 6: Test the System

1. **Login** with `admin` / `admin123`
2. **Navigate** to Dashboard (should show 0 documents, 0 conversations)
3. **Access Swagger UI** at http://localhost:8080/swagger-ui.html
4. **Test Auth Endpoint**:
   - POST `/api/v1/auth/login`
   - Body: `{"username": "admin", "password": "admin123"}`
   - Should return JWT token

---

## Using Docker Compose (Alternative)

To run everything in Docker:

```bash
# Start all services
make docker-up

# Or manually
cd infra
docker-compose up --build
```

Access:
- Frontend: http://localhost:5173
- Backend: http://localhost:8080

**Note**: Building images takes ~5 minutes first time.

---

## Common Commands

```bash
# Start only infrastructure
make infra-up

# Stop infrastructure
make infra-down

# View logs
make logs

# Reset database (deletes all data!)
make reset

# Run backend tests
make test-backend

# Clean build artifacts
make clean

# Show all commands
make help
```

---

## Troubleshooting

### Database Connection Failed
```bash
# Restart PostgreSQL
docker restart ekoc-postgres

# Check logs
docker logs ekoc-postgres
```

### Port Already in Use
- **8080**: Another app using this port. Stop it or change `SERVER_PORT` in `.env`
- **5432**: PostgreSQL conflict. Stop other PostgreSQL instances
- **5173**: Another Vite server. Kill it: `killall node`

### Gradle Build Failed
```bash
# Clean and rebuild
cd backend
./gradlew clean build
```

### Frontend Won't Start
```bash
# Delete node_modules and reinstall
cd frontend
rm -rf node_modules package-lock.json
npm install
```

### Migrations Failed
```bash
# Reset database and restart backend
make reset
cd infra && docker-compose up -d postgres
cd ../backend && ./gradlew bootRun
```

---

## Next Steps

Once running successfully:

1. **Read Architecture Docs**: `docs/architecture/01-overview.md`
2. **Review ADRs**: `docs/adr/`
3. **Check Implementation Roadmap**: `IMPLEMENTATION_ROADMAP.md`
4. **Start Implementing**: Begin with Phase 1, Task 1 (Spring AI Integration)

---

## Development Workflow

### Daily Startup
```bash
# Terminal 1: Infrastructure (leave running)
make infra-up

# Terminal 2: Backend
cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'

# Terminal 3: Frontend
cd frontend && npm run dev
```

### Making Changes

**Backend Changes**:
- Spring Boot DevTools auto-reloads
- Or restart: `Ctrl+C` and re-run `bootRun`

**Frontend Changes**:
- Vite hot-reloads automatically
- No restart needed

**Database Changes**:
- Create new migration: `V{n}__description.sql` in `backend/src/main/resources/db/migration/`
- Restart backend to apply

---

## Stopping Services

```bash
# Stop backend: Ctrl+C in backend terminal
# Stop frontend: Ctrl+C in frontend terminal

# Stop infrastructure
make infra-down

# Or stop everything
cd infra && docker-compose down

# Stop and remove all data
cd infra && docker-compose down -v
```

---

## Project Status

### ✅ Complete
- Project structure and build configuration
- Spring Security with JWT authentication
- Database schema and migrations
- AI abstraction layer (interfaces)
- Frontend shell with routing
- Docker Compose infrastructure
- Documentation and ADRs

### 🚧 To Implement
- Spring AI embedding service
- Document ingestion pipeline
- Vector search with pgvector
- Multi-agent chat orchestration
- Document upload UI
- Chat interface UI

See `IMPLEMENTATION_ROADMAP.md` for detailed task list.

---

## Getting Help

- **Architecture**: See `docs/architecture/`
- **ADRs**: See `docs/adr/`
- **Backend README**: `backend/README.md`
- **Issues**: Create an issue in the project repo

---

## Production Deployment

**DO NOT use default credentials in production!**

Before deploying:
1. Change `JWT_SECRET` to a strong random value (min 256 bits)
2. Change default admin password
3. Set strong database passwords
4. Configure TLS/HTTPS
5. Set up proper monitoring
6. Review security checklist in `docs/security-checklist.md` (TODO)

---

Happy coding! 🚀
