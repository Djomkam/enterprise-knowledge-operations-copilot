#!/bin/bash

set -e

echo "========================================="
echo "Resetting Database"
echo "========================================="

read -p "This will delete all data. Are you sure? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]
then
    echo "Aborted."
    exit 1
fi

cd infra
docker-compose down -v
docker-compose up -d postgres

echo ""
echo "Database reset complete!"
echo "Restart backend to run migrations."
echo ""
