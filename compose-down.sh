#!/bin/bash

# Parse flags
VOLUMES_FLAG=""
if [ "$1" = "-v" ]; then
    VOLUMES_FLAG="-v"
    echo "=== Stopping Balance Tracker infrastructure (with volume removal) ==="
else
    echo "=== Stopping Balance Tracker infrastructure ==="
fi

docker compose down $VOLUMES_FLAG

if [ $? -eq 0 ]; then
    echo ""
    echo "=== Infrastructure stopped successfully ==="
    if [ -n "$VOLUMES_FLAG" ]; then
        echo "PostgreSQL data volume has been removed"
    else
        echo "Note: PostgreSQL data is preserved in volume 'postgres_data'"
        echo "To remove data volume: ./compose-down.sh -v"
    fi
else
    echo ""
    echo "=== Failed to stop infrastructure ==="
    exit 1
fi
