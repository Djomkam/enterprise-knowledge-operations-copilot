#!/bin/bash

# This script generates the remaining backend skeleton files
# Run from project root

BASE_DIR="backend/src/main/java/io/innovation/ekoc"

# Repositories
mkdir -p "$BASE_DIR/users/repository"
mkdir -p "$BASE_DIR/teams/repository"
mkdir -p "$BASE_DIR/documents/repository"
mkdir -p "$BASE_DIR/chat/repository"
mkdir -p "$BASE_DIR/memory/repository"
mkdir -p "$BASE_DIR/audit/repository"

# Services
mkdir -p "$BASE_DIR/users/service"
mkdir -p "$BASE_DIR/teams/service"
mkdir -p "$BASE_DIR/documents/service"
mkdir -p "$BASE_DIR/ingestion/service"
mkdir -p "$BASE_DIR/ingestion/processor"
mkdir -p "$BASE_DIR/retrieval/service"
mkdir -p "$BASE_DIR/chat/service"
mkdir -p "$BASE_DIR/memory/service"
mkdir -p "$BASE_DIR/audit/service"
mkdir -p "$BASE_DIR/audit/aspect"
mkdir -p "$BASE_DIR/admin/service"

# Controllers
mkdir -p "$BASE_DIR/auth/controller"
mkdir -p "$BASE_DIR/users/controller"
mkdir -p "$BASE_DIR/teams/controller"
mkdir -p "$BASE_DIR/documents/controller"
mkdir -p "$BASE_DIR/chat/controller"
mkdir -p "$BASE_DIR/memory/controller"
mkdir -p "$BASE_DIR/admin/controller"

# DTOs
mkdir -p "$BASE_DIR/auth/dto"
mkdir -p "$BASE_DIR/users/dto"
mkdir -p "$BASE_DIR/teams/dto"
mkdir -p "$BASE_DIR/documents/dto"
mkdir -p "$BASE_DIR/ingestion/dto"
mkdir -p "$BASE_DIR/retrieval/dto"
mkdir -p "$BASE_DIR/chat/dto"
mkdir -p "$BASE_DIR/memory/dto"
mkdir -p "$BASE_DIR/admin/dto"

echo "Backend directory structure created successfully!"
