#!/bin/bash

set -e

echo "========================================="
echo "Starting Enterprise Knowledge Operations Copilot"
echo "========================================="

# Check if .env file exists, if not copy from template
if [ ! -f infra/.env ]; then
    echo "Creating .env file from template..."
    cp infra/.env.template infra/.env
    echo "Please edit infra/.env with your configuration"
    echo "Especially set OPENAI_API_KEY if using OpenAI"
fi

# Start infrastructure
echo ""
echo "Starting infrastructure services..."
cd infra
docker-compose up -d postgres rabbitmq minio

echo ""
echo "Waiting for services to be healthy..."
sleep 15

echo ""
echo "Infrastructure is ready!"
echo ""
echo "Next steps:"
echo "  1. Start backend:  cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'"
echo "  2. Start frontend: cd frontend && npm install && npm run dev"
echo ""
echo "Access points:"
echo "  - Frontend:       http://localhost:5173"
echo "  - Backend API:    http://localhost:8080"
echo "  - Swagger UI:     http://localhost:8080/swagger-ui.html"
echo "  - MinIO Console:  http://localhost:9001 (minioadmin/minioadmin)"
echo "  - RabbitMQ Admin: http://localhost:15672 (guest/guest)"
echo ""
echo "Default login: admin / admin123"
echo ""
