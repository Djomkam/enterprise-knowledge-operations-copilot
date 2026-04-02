# Troubleshooting Guide

Common issues and their solutions for Enterprise Knowledge Operations Copilot.

## Port Conflicts

### Error: "Bind for 127.0.0.1:XXXX failed: port is already allocated"

**Cause**: Another process or Docker container is using one of our required ports.

**Ports Used**:
- **5432** - PostgreSQL
- **5672** - RabbitMQ
- **15672** - RabbitMQ Management
- **9090** - MinIO API
- **9091** - MinIO Console
- **11434** - Ollama
- **8080** - Backend API
- **5173** - Frontend

**Solution 1: Use cleanup script**
```bash
./scripts/cleanup-ports.sh
```

**Solution 2: Manual cleanup**
```bash
# Stop all EKOC containers
docker stop ekoc-postgres ekoc-rabbitmq ekoc-minio ekoc-ollama ekoc-backend ekoc-frontend
docker rm ekoc-postgres ekoc-rabbitmq ekoc-minio ekoc-ollama ekoc-backend ekoc-frontend

# Check what's using a specific port
lsof -i :9000
lsof -i :5432

# Kill the process if needed
kill -9 <PID>
```

**Solution 3: Remove all Docker containers**
```bash
# Nuclear option - stops and removes ALL Docker containers
docker stop $(docker ps -aq)
docker rm $(docker ps -aq)
```

---

## Docker Issues

### Error: "Cannot connect to Docker daemon"

**Cause**: Docker Desktop is not running.

**Solution**:
```bash
# Start Docker Desktop
open -a Docker

# Wait for Docker to start (~30 seconds)
# Then verify it's running
docker ps
```

### Error: "docker compose command not found"

**Cause**: Using old Docker version or wrong command.

**Solution**:
```bash
# Try with hyphen (older Docker versions)
docker-compose up -d

# Or update Makefile to use 'docker-compose' instead of 'docker compose'
```

### Slow Docker on Mac

**Solution**:
```bash
# Allocate more resources in Docker Desktop
# Settings → Resources → Advanced
# - CPUs: 4+
# - Memory: 8GB+
# - Swap: 2GB+
```

---

## Ollama Issues

### Ollama container starts but models not available

**Cause**: Models not pulled yet.

**Solution**:
```bash
# Pull required models (takes 5-10 minutes first time)
make ollama-pull

# Or manually:
docker exec ekoc-ollama ollama pull llama2
docker exec ekoc-ollama ollama pull nomic-embed-text

# Verify models are installed
make ollama-models
```

### Ollama health check failing

**Cause**: Ollama takes ~30 seconds to fully start.

**Solution**:
```bash
# Check Ollama logs
docker logs ekoc-ollama

# Restart Ollama if needed
docker restart ekoc-ollama

# Wait 30 seconds and check status
docker ps | grep ollama
```

### Using local Ollama instead of Docker

**Configuration**:
```bash
# Edit backend/src/main/resources/application-local.yml
ai:
  ollama:
    base-url: http://localhost:11434  # Local Ollama

# Or set environment variable
export OLLAMA_BASE_URL=http://localhost:11434
```

---

## Database Issues

### Error: "Connection refused" when backend starts

**Cause**: PostgreSQL not fully started or not accepting connections.

**Solution**:
```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# Check PostgreSQL logs
docker logs ekoc-postgres

# Check if it's healthy
docker inspect ekoc-postgres | grep -A 5 Health

# Restart PostgreSQL
docker restart ekoc-postgres
sleep 10
```

### Flyway migration fails

**Cause**: Database schema corrupted or migrations out of order.

**Solution**:
```bash
# Option 1: Reset database (DELETES ALL DATA)
make reset
cd infra && docker compose up -d postgres
sleep 10

# Option 2: Connect and check manually
docker exec -it ekoc-postgres psql -U ekoc -d ekoc
\dt  -- List tables
\q   -- Exit
```

### pgvector extension not installed

**Cause**: Using wrong PostgreSQL image.

**Solution**:
```bash
# Verify using pgvector image
docker inspect ekoc-postgres | grep Image
# Should show: pgvector/pgvector:pg16

# If wrong image, remove and recreate
make reset
make infra-up
```

---

## Backend Issues

### Backend fails to start - "Could not resolve dependencies"

**Cause**: Gradle cache issue or network problem.

**Solution**:
```bash
cd backend

# Clean and rebuild
./gradlew clean build --refresh-dependencies

# If still fails, delete Gradle cache
rm -rf ~/.gradle/caches
./gradlew build
```

### Backend starts but endpoints return 401 Unauthorized

**Cause**: JWT authentication is working correctly - you need to login first.

**Solution**:
```bash
# Get JWT token via login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Use the returned token
curl http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer <token>"
```

### Backend can't connect to MinIO

**Cause**: MinIO not started or wrong endpoint.

**Solution**:
```bash
# Check MinIO is running
docker ps | grep minio

# Test MinIO is accessible
curl http://localhost:9090/minio/health/live

# Check MinIO logs
docker logs ekoc-minio

# Verify endpoint in application.yml
# Should be: http://localhost:9090 (local) or http://minio:9000 (Docker internal)
```

---

## Frontend Issues

### Frontend build fails - "Module not found"

**Cause**: Dependencies not installed.

**Solution**:
```bash
cd frontend

# Remove old dependencies
rm -rf node_modules package-lock.json

# Reinstall
npm install

# Try again
npm run dev
```

### Frontend can't connect to backend

**Cause**: Backend not running or CORS issue.

**Solution**:
```bash
# 1. Verify backend is running
curl http://localhost:8080/actuator/health

# 2. Check browser console for errors

# 3. Verify CORS configuration in SecurityConfig.java
# Should allow: http://localhost:5173

# 4. Check frontend API client configuration
# Should use: http://localhost:8080
```

### Vite dev server won't start - "Port 5173 in use"

**Solution**:
```bash
# Find what's using port 5173
lsof -i :5173

# Kill it
kill -9 <PID>

# Or use different port
npm run dev -- --port 3000
```

---

## Performance Issues

### Slow startup (>5 minutes)

**Possible causes**:
1. First-time Ollama model download
2. Docker resource constraints
3. Slow network

**Solutions**:
```bash
# 1. Check Docker resources (see Docker Issues above)

# 2. Skip Ollama if using OpenAI
cd infra
docker compose up -d postgres rabbitmq minio
# Don't start Ollama

# 3. Use pre-pulled images
docker pull pgvector/pgvector:pg16
docker pull rabbitmq:3.12-management-alpine
docker pull minio/minio:latest
docker pull ollama/ollama:latest
```

### High CPU usage

**Cause**: Ollama model inference or Gradle compilation.

**Solution**:
```bash
# Check resource usage
docker stats

# If Ollama is the culprit and not needed:
docker stop ekoc-ollama

# Use OpenAI instead
export AI_PROVIDER=openai
export OPENAI_API_KEY=your-key
```

---

## Development Workflow Issues

### Changes not reflected after edit

**Backend**:
```bash
# Spring Boot DevTools should auto-reload
# If not, restart manually:
# Ctrl+C in terminal, then:
./gradlew bootRun
```

**Frontend**:
```bash
# Vite should hot-reload automatically
# If not, check browser console for errors
# Or restart dev server:
# Ctrl+C, then:
npm run dev
```

### Tests fail with "Connection refused"

**Cause**: Tests trying to connect to non-existent database.

**Solution**:
```bash
# Tests should use Testcontainers (embedded DB)
# If you see connection errors, check test configuration

# Run tests with local DB:
make infra-up  # Start postgres
cd backend
./gradlew test
```

---

## Network Issues

### Services can't communicate (Docker networking)

**Cause**: Services in different Docker networks.

**Solution**:
```bash
# Check network
docker network ls
docker network inspect infra_default

# All services should be on same network
# If not, recreate:
make reset
make infra-up
```

### Can't access services from host machine

**Cause**: Ports bound to wrong interface.

**Solution**:
```bash
# Verify ports are bound to 127.0.0.1 or 0.0.0.0
docker ps

# Should show:
# 127.0.0.1:5432->5432/tcp  (correct)
# NOT: 5432/tcp (won't be accessible)

# If incorrect, check docker-compose.yml port bindings
```

---

## Security Issues

### Warning: "JWT_SECRET not set"

**Solution**:
```bash
# For local development:
export JWT_SECRET="local-dev-secret-change-in-production-at-least-256-bits"

# For production: Generate a strong secret
openssl rand -base64 64

# Add to .env file
echo "JWT_SECRET=<generated-secret>" >> infra/.env
```

### Can't login with default credentials

**Cause**: Database not migrated or wrong credentials.

**Solution**:
```bash
# 1. Check database migrations ran
docker logs ekoc-backend | grep Flyway

# 2. Verify user exists
docker exec -it ekoc-postgres psql -U ekoc -d ekoc
SELECT username FROM users;
\q

# 3. Reset if needed
make reset
make infra-up
# Wait for backend to start and run migrations
```

---

## Getting More Help

### View Logs

```bash
# All services
make logs

# Specific service
docker logs ekoc-postgres
docker logs ekoc-backend
docker logs ekoc-ollama

# Follow logs (real-time)
docker logs -f ekoc-backend

# Last 100 lines
docker logs --tail 100 ekoc-backend
```

### Check Service Health

```bash
# All containers
docker ps

# Specific health checks
docker inspect ekoc-postgres | grep -A 10 Health
docker inspect ekoc-ollama | grep -A 10 Health

# Backend health endpoint
curl http://localhost:8080/actuator/health
```

### Debug Mode

```bash
# Backend with debug logging
cd backend
./gradlew bootRun --args='--spring.profiles.active=local --debug'

# Or edit application-local.yml:
logging:
  level:
    io.innovation.ekoc: DEBUG
    org.springframework: DEBUG
```

### Clean Slate

```bash
# Complete reset (DELETES EVERYTHING)
make reset                    # Remove volumes
make clean                    # Remove build artifacts
docker system prune -a       # Remove all Docker data
make infra-up                # Start fresh
```

---

## Still Having Issues?

1. **Check documentation**:
   - `QUICKSTART.md` - Getting started
   - `README.md` - Project overview
   - `docs/architecture/` - Architecture details

2. **Check configuration**:
   - `infra/.env.template` - Environment variables
   - `backend/src/main/resources/application.yml` - Backend config
   - `infra/docker-compose.yml` - Infrastructure setup

3. **Verify prerequisites**:
   ```bash
   java --version    # Should be 21+
   docker --version  # Should be recent
   node --version    # Should be 20+
   ```

4. **Create an issue** with:
   - Error message
   - Steps to reproduce
   - Output of `docker ps`
   - Relevant logs
