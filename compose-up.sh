#!/bin/bash

echo "=== Starting Balance Tracker infrastructure (PostgreSQL) ==="
docker compose up -d

if [ $? -eq 0 ]; then
    echo ""
    echo "=== Infrastructure started successfully ==="
    echo "PostgreSQL is running on localhost:5432"
    echo "Database: balancetracker"
    echo "Username: balancetracker"
    echo "Password: balancetracker"
    echo "Data is persisted in Docker volume: postgres_data"
    echo ""
    echo "To view logs: docker compose logs -f"
    echo "To stop: ./compose-down.sh"
else
    echo ""
    echo "=== Failed to start infrastructure ==="
    exit 1
fi
