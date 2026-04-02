#!/bin/bash

set -e

echo "========================================="
echo "Stopping Enterprise Knowledge Operations Copilot"
echo "========================================="

cd infra
docker-compose down

echo ""
echo "All services stopped!"
echo ""
echo "To remove all data (databases, volumes):"
echo "  cd infra && docker-compose down -v"
echo ""
