# Docker Configuration Fixes

## Issues Fixed

### 1. Port Conflict Resolution
**Problem**: Ports were binding to 0.0.0.0 (all interfaces), causing conflicts with other services

**Solution**: Changed all port bindings to localhost (127.0.0.1) only

**Changes**:
```yaml
# Before
ports:
  - "5432:5432"      # Binds to all interfaces
  - "9000:9000"

# After
ports:
  - "127.0.0.1:5432:5432"  # Binds to localhost only
  - "127.0.0.1:9000:9000"
```

**Benefits**:
- ✅ Prevents conflicts with system services
- ✅ More secure (not exposed on network)
- ✅ Allows multiple Docker Compose projects

### 2. Ollama Integration Added
**Requirement**: Use Ollama for local LLM instead of OpenAI

**Solution**: Added Ollama service to docker-compose.yml

**Service Configuration**:
```yaml
ollama:
  image: ollama/ollama:latest
  container_name: ekoc-ollama
  ports:
    - "127.0.0.1:11434:11434"
  volumes:
    - ollama_data:/root/.ollama
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:11434/api/tags"]
    interval: 30s
    timeout: 10s
    retries: 5
    start_period: 30s
  restart: unless-stopped
```

**Backend Integration**:
```yaml
backend:
  environment:
    OLLAMA_BASE_URL: http://ollama:11434
    AI_PROVIDER: ${AI_PROVIDER:-ollama}
  depends_on:
    ollama:
      condition: service_healthy
```

### 3. Added restart Policies
**Problem**: Services wouldn't restart after Docker restart or system reboot

**Solution**: Added `restart: unless-stopped` to all services

**Services Updated**:
- postgres
- rabbitmq
- minio
- ollama
- backend (when using Docker)
- frontend (when using Docker)

---

## Updated Port Mappings

| Service | Host Port | Container Port | Binding |
|---------|-----------|----------------|---------|
| PostgreSQL | 5432 | 5432 | 127.0.0.1 |
| RabbitMQ | 5672 | 5672 | 127.0.0.1 |
| RabbitMQ Admin | 15672 | 15672 | 127.0.0.1 |
| MinIO API | 9090 | 9000 | 127.0.0.1 |
| MinIO Console | 9091 | 9091 | 127.0.0.1 |
| **Ollama** | **11434** | **11434** | **127.0.0.1** |
| Backend | 8080 | 8080 | All interfaces |
| Frontend | 5173 | 5173 | All interfaces |

**Note**: Backend and Frontend remain accessible on all interfaces for development convenience.

---

## New Makefile Commands

### Ollama Commands Added

```bash
make ollama-pull      # Pull recommended models (llama2, nomic-embed-text)
make ollama-models    # List installed models
make ollama-shell     # Open shell in Ollama container
```

### Updated Commands

```bash
make infra-up         # Now includes Ollama
make help             # Shows new Ollama commands
```

---

## New Scripts

### cleanup-ports.sh
Location: `scripts/cleanup-ports.sh`

**Purpose**: Clean up Docker containers and check for port conflicts

**Usage**:
```bash
./scripts/cleanup-ports.sh
```

**What it does**:
1. Stops all EKOC containers
2. Removes all EKOC containers
3. Checks which ports are in use
4. Shows PIDs of processes using EKOC ports
5. Provides commands to kill blocking processes

---

## Ollama Setup

### First Time Setup

```bash
# 1. Start infrastructure (includes Ollama)
make infra-up

# 2. Pull models (takes 5-10 minutes)
make ollama-pull

# 3. Verify models installed
make ollama-models
```

### Recommended Models

| Model | Purpose | Size | Use Case |
|-------|---------|------|----------|
| **llama2** | Chat completions | ~4GB | Conversational AI, RAG responses |
| **nomic-embed-text** | Text embeddings | ~274MB | Document embeddings, similarity search |

### Advanced Usage

```bash
# Pull specific model version
docker exec ekoc-ollama ollama pull llama2:13b

# List all available models
docker exec ekoc-ollama ollama list

# Remove a model
docker exec ekoc-ollama ollama rm llama2

# Test Ollama API
curl http://localhost:11434/api/tags

# Generate text (test)
curl http://localhost:11434/api/generate -d '{
  "model": "llama2",
  "prompt": "Why is the sky blue?",
  "stream": false
}'
```

---

## Environment Configuration

### Using Ollama (Default)

```bash
# In infra/.env
AI_PROVIDER=ollama
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_CHAT_MODEL=llama2
OLLAMA_EMBEDDING_MODEL=nomic-embed-text
```

### Using OpenAI

```bash
# In infra/.env
AI_PROVIDER=openai
OPENAI_API_KEY=sk-your-key-here
OPENAI_CHAT_MODEL=gpt-4-turbo-preview
OPENAI_EMBEDDING_MODEL=text-embedding-3-small
```

### Switching Between Providers

```bash
# Switch to OpenAI
export AI_PROVIDER=openai
export OPENAI_API_KEY=sk-xxx

# Switch back to Ollama
export AI_PROVIDER=ollama
export OLLAMA_BASE_URL=http://localhost:11434

# Restart backend to apply changes
```

---

## Troubleshooting

### Port Already in Use

**Error**:
```
Bind for 127.0.0.1:9000 failed: port is already allocated
```

**Solution**:
```bash
# Option 1: Use cleanup script
./scripts/cleanup-ports.sh

# Option 2: Find and kill process
lsof -i :9000
kill -9 <PID>

# Option 3: Stop all containers
docker stop $(docker ps -aq)
docker rm $(docker ps -aq)
```

### Ollama Models Not Available

**Error**: Backend can't connect to Ollama or models not found

**Solution**:
```bash
# 1. Verify Ollama is running
docker ps | grep ollama

# 2. Check Ollama logs
docker logs ekoc-ollama

# 3. Pull models if missing
make ollama-pull

# 4. Verify models
make ollama-models

# 5. Test API
curl http://localhost:11434/api/tags
```

### Ollama Taking Too Long to Start

**Cause**: Health check waiting for Ollama to be fully ready

**Solution**:
```bash
# Ollama has a 30-second start_period in health check
# This is normal - just wait

# Check status
docker inspect ekoc-ollama | grep -A 10 Health

# If stuck, restart
docker restart ekoc-ollama
```

### Services Can't Communicate

**Error**: Backend can't reach Ollama at http://ollama:11434

**Cause**: Services on different Docker networks or wrong endpoint

**Solution**:
```bash
# 1. Verify all services on same network
docker network inspect infra_default

# 2. Check backend environment
docker exec ekoc-backend env | grep OLLAMA

# 3. From backend, test Ollama connectivity
docker exec ekoc-backend curl http://ollama:11434/api/tags

# 4. If fails, recreate network
make reset
make infra-up
```

---

## Performance Considerations

### Ollama Resource Usage

**CPU**: Ollama can use significant CPU during inference
- llama2: ~2-4 CPU cores during generation
- nomic-embed-text: ~1 CPU core during embedding

**Memory**: Models are loaded into RAM
- llama2 (7B): ~4GB RAM
- nomic-embed-text: ~500MB RAM

**Recommendations**:
```bash
# Docker Desktop Settings → Resources
- CPUs: 4+ (6+ recommended with Ollama)
- Memory: 8GB+ (12GB+ recommended with Ollama)
- Swap: 2GB+
```

### Startup Time

| Service | Startup Time | Notes |
|---------|--------------|-------|
| PostgreSQL | ~5 seconds | Quick |
| RabbitMQ | ~10 seconds | Quick |
| MinIO | ~5 seconds | Quick |
| **Ollama** | **~30 seconds** | Slower (health check start_period) |
| Backend | ~20-30 seconds | After dependencies healthy |

**Total**: ~45-60 seconds for full stack

---

## Benefits Summary

### Port Binding to Localhost

✅ **Security**: Services not exposed on network
✅ **Isolation**: Won't conflict with other services
✅ **Flexibility**: Multiple Docker projects can coexist

### Ollama Integration

✅ **No API costs**: Free local inference
✅ **Privacy**: Data never leaves your machine
✅ **Offline capable**: Works without internet
✅ **Fast iteration**: No API rate limits

### Restart Policies

✅ **Reliability**: Services auto-restart
✅ **Persistence**: Survives Docker restarts
✅ **Development**: Don't manually restart after reboot

---

## Migration Guide

### From Old Configuration

If you were using the old configuration:

```bash
# 1. Stop everything
make infra-down

# 2. Remove old containers
docker rm ekoc-postgres ekoc-rabbitmq ekoc-minio

# 3. Pull updated configuration (already done)

# 4. Start with new configuration
make infra-up

# 5. Pull Ollama models
make ollama-pull

# 6. Verify everything works
docker ps
make ollama-models
```

---

## See Also

- **TROUBLESHOOTING.md** - Comprehensive troubleshooting guide
- **QUICKSTART.md** - Updated with Ollama instructions
- **infra/docker-compose.yml** - Full service configuration
- **Makefile** - All available commands
