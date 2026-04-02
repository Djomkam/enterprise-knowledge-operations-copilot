#!/bin/bash

# Script to clean up any existing Docker containers that might be using our ports

echo "========================================="
echo "Cleaning up potential port conflicts"
echo "========================================="

# Stop and remove any existing EKOC containers
echo ""
echo "Stopping existing EKOC containers..."
docker stop ekoc-postgres ekoc-rabbitmq ekoc-minio ekoc-ollama ekoc-backend ekoc-frontend 2>/dev/null || true
docker rm ekoc-postgres ekoc-rabbitmq ekoc-minio ekoc-ollama ekoc-backend ekoc-frontend 2>/dev/null || true

# Find and list processes using our ports
echo ""
echo "Checking for processes using EKOC ports..."
echo ""

check_port() {
    local port=$1
    local service=$2
    echo -n "Port $port ($service): "
    local pid=$(lsof -ti:$port 2>/dev/null)
    if [ -z "$pid" ]; then
        echo "✓ Available"
    else
        echo "✗ In use by PID $pid"
        echo "  To kill: kill -9 $pid"
    fi
}

check_port 5432 "PostgreSQL"
check_port 5672 "RabbitMQ"
check_port 15672 "RabbitMQ Admin"
check_port 9000 "MinIO API"
check_port 9001 "MinIO Console"
check_port 11434 "Ollama"
check_port 8080 "Backend"
check_port 5173 "Frontend"

echo ""
echo "Cleanup complete!"
echo ""
echo "If any ports are still in use, you can:"
echo "  1. Kill the process: kill -9 <PID>"
echo "  2. Or wait for it to release the port"
echo ""
echo "Then run: make infra-up"
echo ""
