.PHONY: help infra-up infra-down backend-up frontend-up dev-up clean reset logs ollama-pull ollama-models

help: ## Show this help message
	@echo 'Usage: make [target]'
	@echo ''
	@echo 'Available targets:'
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'

infra-up: ## Start infrastructure services (postgres, rabbitmq, minio, ollama)
	@echo "Starting infrastructure services..."
	cd infra && docker compose up -d postgres rabbitmq minio ollama
	@echo "Waiting for services to be healthy..."
	@sleep 15
	@echo "Infrastructure ready!"
	@echo ""
	@echo "Note: To use Ollama, pull models with 'make ollama-pull'"

infra-down: ## Stop infrastructure services
	@echo "Stopping infrastructure services..."
	cd infra && docker compose down
	@echo "Infrastructure stopped!"

backend-up: ## Start backend application
	@echo "Starting backend..."
	./gradlew :backend:bootRun --args='--spring.profiles.active=local'

frontend-up: ## Start frontend application
	@echo "Starting frontend..."
	cd frontend && npm install && npm run dev

dev-up: ## Start full stack (infra + backend + frontend)
	@echo "Starting full development environment..."
	$(MAKE) infra-up
	@echo "Infrastructure ready. Start backend and frontend in separate terminals:"
	@echo "  Terminal 1: make backend-up"
	@echo "  Terminal 2: make frontend-up"

docker-up: ## Start all services with Docker Compose
	@echo "Starting all services with Docker Compose..."
	cd infra && docker compose up --build

docker-down: ## Stop all Docker Compose services
	@echo "Stopping all Docker Compose services..."
	cd infra && docker compose down

clean: ## Clean build artifacts
	@echo "Cleaning build artifacts..."
	./gradlew :backend:clean
	cd frontend && rm -rf node_modules dist
	@echo "Clean complete!"

reset: ## Reset database and volumes
	@echo "Resetting database and volumes..."
	cd infra && docker compose down -v
	@echo "Reset complete!"

logs: ## Show logs from infrastructure services
	cd infra && docker compose logs -f

test-backend: ## Run backend tests
	./gradlew :backend:test

build-backend: ## Build backend JAR
	./gradlew :backend:build

build-frontend: ## Build frontend
	cd frontend && npm run build

ollama-pull: ## Pull recommended Ollama models (llama2, nomic-embed-text)
	@echo "Pulling Ollama models (this may take several minutes)..."
	@docker exec ekoc-ollama ollama pull llama2
	@docker exec ekoc-ollama ollama pull nomic-embed-text
	@echo "Models pulled successfully!"
	@echo "Available models:"
	@docker exec ekoc-ollama ollama list

ollama-models: ## List installed Ollama models
	@docker exec ekoc-ollama ollama list

ollama-shell: ## Open shell in Ollama container
	@docker exec -it ekoc-ollama /bin/bash

.DEFAULT_GOAL := help
